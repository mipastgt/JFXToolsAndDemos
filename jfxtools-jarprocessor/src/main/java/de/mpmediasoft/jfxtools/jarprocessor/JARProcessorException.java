package de.mpmediasoft.jfxtools.jarprocessor;

@SuppressWarnings("serial")
public class JARProcessorException extends Exception {

    public JARProcessorException() {
    }

    public JARProcessorException(String message) {
        super(message);
    }

    public JARProcessorException(Throwable cause) {
        super(cause);
    }

    public JARProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

}
