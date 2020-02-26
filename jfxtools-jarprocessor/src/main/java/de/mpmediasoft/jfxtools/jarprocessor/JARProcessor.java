package de.mpmediasoft.jfxtools.jarprocessor;

import java.util.spi.ToolProvider;

public interface JARProcessor {
    
    public void initialize(ToolProvider jarToolProvider, boolean verbose);
    
    public void start();
    
    public void process(String arg) throws JARProcessorException;
    
    public void finish();
    
}
