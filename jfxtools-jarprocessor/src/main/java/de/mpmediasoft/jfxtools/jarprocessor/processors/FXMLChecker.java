package de.mpmediasoft.jfxtools.jarprocessor.processors;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.spi.ToolProvider;

import de.mpmediasoft.jfxtools.jarprocessor.AbstractJARProcessor;
import de.mpmediasoft.jfxtools.jarprocessor.JARProcessorException;

/**
 * This jar-processor analyzes a list of jar-files and lists all FXML files
 * contained in each file.
 * 
 * It further examines all import statements of the FXML files and finally
 * prints out a combined list of all classes loaded by all the jar files.
 * This is useful for tools like the maven shade plugin, ProGuard of GluonHQs
 * client-maven-plugin for which you have to provide a list of all classes
 * which have to be kept but which cannot be found by static analysis of the
 * code.
 * 
 * This code needs Java 11+.
 * 
 * @author mpaus
 */
public class FXMLChecker extends AbstractJARProcessor {

    private final static String indent = "   ";
    
    public static enum TargetFormat {PLAIN, MAVEN_SHADE, MAVEN_GLUON_NATIVE}
    
    private final static Map<TargetFormat, String> formatMap = Map.of(
        TargetFormat.PLAIN, "%s\n",
        TargetFormat.MAVEN_SHADE, "<include>%s</include>\n",
        TargetFormat.MAVEN_GLUON_NATIVE, "<list>%s</list>\n"
    );
    
    private final Set<String> fxmlClasses = new HashSet<>();
    
    private ToolProvider jar;
    
    private TargetFormat targetFormat = TargetFormat.PLAIN;
    
    private boolean verbose = false;
    
    private int errors = 0;
    
    @Override
    public void initialize(ToolProvider jarToolProvider, boolean verbose) {
        this.jar = jarToolProvider;
        this.verbose = verbose;
    }

    @Override
    public void start() {
        fxmlClasses.clear();
        errors = 0;
    }

    @Override
    public void process(String arg) throws JARProcessorException {
        if (arg.startsWith("-")) {
            if (arg.equals("-p")) targetFormat = TargetFormat.PLAIN;
            if (arg.equals("-s")) targetFormat = TargetFormat.MAVEN_SHADE;
            if (arg.equals("-n")) targetFormat = TargetFormat.MAVEN_GLUON_NATIVE;
        } else {
            File jarFile = jarFile(arg);
            if (verbose) System.out.println("JAR: " + jarFile);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = new PrintStream(baos);
    
            String[] jarListArgs = { "--list", "--file", jarFile.getAbsolutePath() };
    
            int status = jar.run(ps, System.err, jarListArgs);
    
            if (status == 0) {
                String res = new String(baos.toByteArray());
                res.lines().filter(s -> {
                    return s.endsWith(".fxml");
                }).forEach(l -> {
                    try {
                        processFXML(jarFile, l);
                    } catch (JARProcessorException e) {
                        ++errors;
                        if (verbose) e.printStackTrace();
                    }
                });
                if (errors > 0) throw new JARProcessorException("process terminated with errors.");
            } else {
                throw new JARProcessorException("jar tool terminated with errors.");
            }
        }
    }

    @Override
    public void finish() {
        if (verbose) System.out.println("====================================");
        if (verbose) System.out.println("Combined list of classes to include.");
        if (verbose) System.out.println("====================================");
        
        String format = formatMap.get(targetFormat);
        fxmlClasses.forEach(c -> System.out.format(format, (targetFormat == TargetFormat.MAVEN_SHADE) ? c.replaceAll("\\.", "/"): c));
    }

    private void processFXML(File jarFile, String line) throws JARProcessorException {
        URI uri = URI.create(String.format("jar:" + jarFile.toURI()));
        try (FileSystem jfs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            Path path = jfs.getPath(line);
            if (Files.isRegularFile(path)) {
                if (verbose) System.out.println(indent + "FXML: " + path);                
                List<String> lines = Files.readAllLines(path);
                for (String impl : lines) {
                    String trImpl = impl.trim();
                    if (trImpl.startsWith("<?import")) {
                        String fxmlClass = trImpl.replace("<?import", "").replace("?>", "").trim();
                        fxmlClasses.add(fxmlClass);
                        if (verbose) System.out.println(indent + indent + "include: " + fxmlClass);
                    }
                }
            } else {
                throw new JARProcessorException("No regular file: " + path);
            }
        } catch (IOException e) {
            throw new JARProcessorException("Error processing line: " + line);
        }
    }
    
}
