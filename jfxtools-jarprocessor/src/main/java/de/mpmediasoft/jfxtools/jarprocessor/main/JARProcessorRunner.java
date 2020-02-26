package de.mpmediasoft.jfxtools.jarprocessor.main;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.spi.ToolProvider;

import de.mpmediasoft.jfxtools.jarprocessor.JARProcessor;
import de.mpmediasoft.jfxtools.jarprocessor.JARProcessorException;
import de.mpmediasoft.jfxtools.jarprocessor.processors.FXMLChecker;
import de.mpmediasoft.jfxtools.jarprocessor.processors.ModuleChecker;

public class JARProcessorRunner {
    
    private static Optional<ToolProvider> jarToolProvider = ToolProvider.findFirst("jar");
    
    private static Map<String, JARProcessor> processorMap = Map.of(
        "ModuleChecker", new ModuleChecker(),
        "FXMLChecker", new FXMLChecker()
    );
    
    private boolean verbose = false;
    
    private JARProcessor jarProcessor;
    
    public JARProcessorRunner(JARProcessor jarProcessor) {
        this.jarProcessor = jarProcessor;
    }
    
    private void process(List<String> argsList) {
        if (jarToolProvider.isPresent()) {
            if (argsList.size() > 0) {
                if (argsList.get(0).equals("-v")) {
                    verbose = true;
                    argsList.remove(0);
                }
                if (argsList.size() > 0) {
                    jarProcessor.initialize(jarToolProvider.get(), verbose);           
                    jarProcessor.start();
                    for (String arg : argsList) {
                        try {
                            jarProcessor.process(arg);
                        } catch (JARProcessorException e) {
                            if (verbose) e.printStackTrace();
                            errorExit("Processing failed for: " + arg);
                        }
                    }
                    jarProcessor.finish();
                }
            } else {
                errorExit("No JAR file specified.");
            }
        } else {
            errorExit("JAR tool not found.");
        }
    }
    
    private static void errorExit(String message) {
        System.err.println(message);
        System.err.println();
        printUsage(System.err);
        System.exit(1);
    }
    
    private static void printUsage(PrintStream ps) {
        ps.println("USAGE: <launcher command> <launcher options> <processor options> <list of jar files>");
        ps.println("  <launcher command> : Command suitable to launch the class JARProcessorRunner. (Depends on packaging.)");
        ps.println("  <launcher options> : Options for the launcher.");
        ps.println("    ModuleChecker | FXMLChecker : Selects the processor.");
        ps.println("    -v : (optional) Makes the output verbose.");
        ps.println("  <processor options> : Options only valid for the selected processor.");
        ps.println("    -p|s|n : (only FXMLChecker) : Output format (P)lain, Maven (S)hade or Maven Gluon (N)ative.");
        ps.println("  <list of jar files> : Space separated list of fully qualified JAR files.");
    }
    
    public static void main(String[] args) {
        // It is important to set the default locale because different
        // Java distributions handle the default language used by tools
        // differently.
        // The output of the 'jar' tool with Oracle JDK is always
        // english whereas AdoptOpenJDK translates the output into
        // the language determined by the default locale, which would make
        // parsing of the results impossible. Therefore we enforce a US locale.
        Locale.setDefault(Locale.US);
        
        List<String> argsList = new ArrayList<String>(Arrays.asList(args)); // We need a mutable list here!
        
        if (argsList.size() > 1) {
            JARProcessor processor = processorMap.get(argsList.remove(0));
            if (processor != null) {
                JARProcessorRunner runner = new JARProcessorRunner(processor);
                runner.process(argsList);
            } else {
                errorExit("No valid processor specified.");
            }
        } else {
            errorExit("Not enough arguments.");
        }
    }

}
