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
import java.util.Set;
import java.util.spi.ToolProvider;

import de.mpmediasoft.jfxtools.jarprocessor.AbstractJARProcessor;
import de.mpmediasoft.jfxtools.jarprocessor.JARProcessorException;

/**
 * This jar-processor analyzes a list of jar-files and lists all native artifacts
 * contained in each file. All files which end with ".o", ".a", ".so", ".dll", ".dylib"
 * or ".jnilib" are listed.
 * 
 * This is useful to know if you want to strip unneeded files from a build or if you want
 * to check if all native artifacts in your build have been properly signed. This is, e.g,
 * necessary to get a bundled app notarized by Apple.
 * 
 * This code needs Java 11+.
 * 
 * @author mpaus
 */
public class NativeArtifactChecker extends AbstractJARProcessor {

    private final static String indent = "   ";
    
    private final Set<String> fxmlClasses = new HashSet<>();
    
    private ToolProvider jar;
    
    private boolean verbose = false;
    
    private int errors = 0;
    
    private int counter = 0;
    
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
        File jarFile = jarFile(arg);
        if (verbose) System.out.println("JAR: " + jarFile);
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);

        String[] jarListArgs = { "--list", "--file", jarFile.getAbsolutePath() };

        int status = jar.run(ps, System.err, jarListArgs);

        if (status == 0) {
            String res = new String(baos.toByteArray());
            res.lines().filter(s -> {
                return
                    s.endsWith(".o") ||
                    s.endsWith(".a") ||
                    s.endsWith(".so") ||
                    s.endsWith(".dll") ||
                    s.endsWith(".dylib") ||
                    s.endsWith(".jnilib");
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

    @Override
    public void finish() {
        if (verbose) System.out.println(counter + " native artifacts found.");        
    }

    private void processFXML(File jarFile, String line) throws JARProcessorException {
        ++counter;
        URI uri = URI.create(String.format("jar:" + jarFile.toURI()));
        try (FileSystem jfs = FileSystems.newFileSystem(uri, Collections.emptyMap())) {
            Path path = jfs.getPath(line);
            if (Files.isRegularFile(path)) {
                System.out.println(indent + path);                
            } else {
                throw new JARProcessorException("No regular file: " + path);
            }
        } catch (IOException e) {
            throw new JARProcessorException("Error processing line: " + line);
        }
    }
    
}
