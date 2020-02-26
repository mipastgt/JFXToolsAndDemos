package de.mpmediasoft.jfxtools.jarprocessor;

import java.io.File;

abstract public class AbstractJARProcessor implements JARProcessor {

    protected File jarFile(String arg) throws JARProcessorException {
        File jarFile = new File(arg);
        if (!jarFile.canRead()) {
            throw new JARProcessorException("File does not exist: " + jarFile);
        } else if (!jarFile.getName().toLowerCase().endsWith(".jar")) {
            throw new JARProcessorException("File does not seem to be a JAR file: " + jarFile);
        } else {
            return jarFile;
        }
    }
    
}
