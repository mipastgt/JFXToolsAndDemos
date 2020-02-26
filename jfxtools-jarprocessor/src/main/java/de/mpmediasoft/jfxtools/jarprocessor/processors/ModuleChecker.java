package de.mpmediasoft.jfxtools.jarprocessor.processors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.Manifest;
import java.util.spi.ToolProvider;
import java.util.stream.Collectors;

import de.mpmediasoft.jfxtools.jarprocessor.AbstractJARProcessor;
import de.mpmediasoft.jfxtools.jarprocessor.JARProcessorException;

/**
 * This jar-processor analyzes a list of jar-files and lists all services provided
 * by the contained code.
 * 
 * It further checks whether the contained code is modular or non-modular. If it is
 * modular, it checks if the service delarations for the module-path are consistent
 * with the declarations for the classpath.
 * 
 * This code needs Java 11+.
 * 
 * @author mpaus
 */
public class ModuleChecker extends AbstractJARProcessor {

    private final static String indent = "   ";
    
    private final static Name AUTOMATIC_MODULE_NAME_KEY = new Name("Automatic-Module-Name");

    private final List<CheckResult> checkResults = new ArrayList<>();
    
    private ToolProvider jar;
    
    private boolean verbose = false;
    
    private int errors = 0;
    
    private static class CheckResult {
        private final File jarFile;
        private final Map<String, Set<String>> modularServiceProviders = new HashMap<>();
        private final Map<String, Set<String>> nonModularServiceProviders = new HashMap<>();
        private boolean modular;
        private boolean consistent;
        private boolean definedAutomaticModuleName;
        
        public boolean hasDefinedAutomaticModuleName() {
            return definedAutomaticModuleName;
        }

        public void setDefinedAutomaticModuleName(boolean definedAutomaticModuleName) {
            this.definedAutomaticModuleName = definedAutomaticModuleName;
        }

        CheckResult(File jarFile) {
            this.jarFile = jarFile;
        }

        boolean isModular() {
            return modular;
        }

        void setModular(boolean modular) {
            this.modular = modular;
        }

        boolean isConsistent() {
            return consistent;
        }

        void setConsistent(boolean consistent) {
            this.consistent = consistent;
        }

        File getJarFile() {
            return jarFile;
        }

        Map<String, Set<String>> getModularServiceProviders() {
            return modularServiceProviders;
        }

        Map<String, Set<String>> getNonModularServiceProviders() {
            return nonModularServiceProviders;
        }
        
    }

    private void showResult(List<CheckResult> checkResults, boolean modular) {
        checkResults.stream().filter(r -> r.isModular() == modular).forEach(r -> {compare(r); printResult(r);});
    }

    private void check(CheckResult checkResult) throws JARProcessorException {
        processMetaInfManifest(checkResult);
        findModularServiceProviders(checkResult);
        findNonModularServiceProviders(checkResult);
    }

    private void findModularServiceProviders(CheckResult checkResult) throws JARProcessorException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        String[] jarDescribeModuleArgs = { "--describe-module", "--file", checkResult.getJarFile().getAbsolutePath() };

        int status = jar.run(ps, System.err, jarDescribeModuleArgs);

        if (status == 0) {
            String res = new String(baos.toByteArray());
            checkResult.setModular(!res.startsWith("No module descriptor found"));
            if (checkResult.isModular()) {
                res.lines().filter(s -> s.startsWith("provides")).forEach(l -> {
                    try {
                        scanProvidesLine(checkResult, l);
                    } catch (JARProcessorException e) {
                        ++errors;
                        if (verbose) e.printStackTrace();
                    }
                });
                if (errors > 0) throw new JARProcessorException("findModularServiceProviders terminated with errors.");
            }
        } else {
            throw new JARProcessorException("jar tool terminated with errors.");
        }
    }

    private void scanProvidesLine(CheckResult checkResult, String line) throws JARProcessorException {
        try (Scanner scanner = new Scanner(line.trim())) {
            List<String> list = scanner.tokens().filter(s -> {
                return !s.equalsIgnoreCase("provides") && !s.equalsIgnoreCase("with");
            }).collect(Collectors.toList());
            if (list.size() == 0) {
                return;
            } else if (list.size() == 2) {
                if (!checkResult.getModularServiceProviders().containsKey(list.get(0))) {
                    checkResult.getModularServiceProviders().put(list.get(0), new HashSet<String>());
                }
                assert !list.get(1).contains(",");
                checkResult.getModularServiceProviders().get(list.get(0)).add(list.get(1));
            } else {
                throw new JARProcessorException("Unexpected provides statement: " + line);
            }
        }
    }

    private void findNonModularServiceProviders(CheckResult checkResult) throws JARProcessorException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        String[] jarListArgs = { "--list", "--file", checkResult.getJarFile().getAbsolutePath() };

        int status = jar.run(ps, System.err, jarListArgs);

        if (status == 0) {
            String res = new String(baos.toByteArray());
            res.lines().filter(s -> {
                return s.startsWith("META-INF/services/") && !s.equals("META-INF/services/");
            }).forEach(l -> {
                try {
                    processMetaInfServices(checkResult, l);
                } catch (JARProcessorException e) {
                    ++errors;
                    if (verbose) e.printStackTrace();
                }
            });
            if (errors > 0) throw new JARProcessorException("findNonModularServiceProviders terminated with errors.");
        } else {
            throw new JARProcessorException("jar tool terminated with errors.");
        }
    }

    private void processMetaInfServices(CheckResult checkResult, String line) throws JARProcessorException {
        String spi = line.replaceFirst("META-INF/services/", "").trim();
        if (!spi.isEmpty()) {
            if (!checkResult.getNonModularServiceProviders().containsKey(spi)) {
                checkResult.getNonModularServiceProviders().put(spi, new HashSet<String>());
            }

            URI uri = URI.create(String.format("jar:" + checkResult.getJarFile().toURI()));
            try (FileSystem jfs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
                Path path = jfs.getPath(line);
                if (Files.isRegularFile(path)) {
                    List<String> lines = Files.readAllLines(path);
                    for (String impl : lines) {
                        if (!impl.startsWith("#"))
                            checkResult.getNonModularServiceProviders().get(spi).add(impl);
                    }
                } else {
                    throw new JARProcessorException("No regular file: " + path);
                }
            } catch (IOException e) {
                throw new JARProcessorException("Error processing line: " + line);
            }
        } else {
            throw new JARProcessorException("Unexpected META-INF/services/ line: " + line);
        }
    }

    private void processMetaInfManifest(CheckResult checkResult) throws JARProcessorException {
        URI uri = URI.create(String.format("jar:" + checkResult.getJarFile().toURI()));
        try (FileSystem jfs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            Path path = jfs.getPath("META-INF/MANIFEST.MF");
            if (Files.isRegularFile(path)) {
                try (InputStream is = Files.newInputStream(path)) {
                    Manifest mf = new Manifest(is);
                    Attributes attributes = mf.getMainAttributes();
                    boolean definedAutomaticModuleName = attributes.containsKey(AUTOMATIC_MODULE_NAME_KEY);
                    checkResult.setDefinedAutomaticModuleName(definedAutomaticModuleName);
                } catch (IOException e) {
                    throw new JARProcessorException("Cannot read manifest from: " + checkResult.getJarFile());
                }
            } else {
                throw new JARProcessorException("No regular file: " + path);
            }
        } catch (IOException e) {
            throw new JARProcessorException("Error processing manifest.");
        }
    }

    private void printResult(CheckResult checkResult) {
        System.out.print(checkResult.getJarFile() + " : " + (checkResult.isModular() ? "modular" : "non-modular"));
        System.out.println(checkResult.isModular() ? "" : " hasDefinedAutomaticModuleName = " + checkResult.hasDefinedAutomaticModuleName());
        if (checkResult.isConsistent()) {
            printResult(null, checkResult.getNonModularServiceProviders());
        } else {
            System.out.println(indent + ">>> Inconsistent service declarations detected. <<<");
            printResult("Services provided on module-path:", checkResult.getModularServiceProviders());
            printResult("Services provided on class-path:", checkResult.getNonModularServiceProviders());
        }
    }
    
    private void printResult(String msg, Map<String, Set<String>> serviceProviders) {
        if (msg != null) System.out.println(indent + msg);
        if (!serviceProviders.isEmpty()) {
            for (String spi : serviceProviders.keySet()) {
                Set<String> nonModImpls = serviceProviders.get(spi);
                for (String nonModImpl : nonModImpls) {
                    System.out.println(indent + indent + "provides " + spi + " with " + nonModImpl);
                }
            }
        }
    }

    private void compare(CheckResult checkResult) {
        checkResult.setConsistent(true);
        if (checkResult.isModular()) {
            if (checkResult.getModularServiceProviders().size() == checkResult.getNonModularServiceProviders().size()) {
                for (String spi : checkResult.getModularServiceProviders().keySet()) {
                    Set<String> modImpls = checkResult.getModularServiceProviders().get(spi);
                    Set<String> nonModImpls = checkResult.getNonModularServiceProviders().get(spi);
                    if (nonModImpls != null && modImpls.size() == nonModImpls.size()) {
                        for (String impl : modImpls) {
                            if (!nonModImpls.contains(impl)) {
                                checkResult.setConsistent(false);
                            }
                        }
                    }
                }
            } else {
                checkResult.setConsistent(false);
            }
        }
    }

    @Override
    public void initialize(ToolProvider jarToolProvider, boolean verbose) {
        this.jar = jarToolProvider;
        this.verbose = verbose;
    }

    @Override
    public void start() {
        checkResults.clear();        
        errors = 0;
    }

    @Override
    public void process(String arg) throws JARProcessorException {
        CheckResult checkResult = new CheckResult(jarFile(arg));
        check(checkResult);
        checkResults.add(checkResult);
    }

    @Override
    public void finish() {
        System.out.println("Modular results:");
        System.out.println();
        showResult(checkResults, true);
        
        System.out.println();
        
        System.out.println("Non-modular results:");
        System.out.println();
        showResult(checkResults, false);            
    }

}
