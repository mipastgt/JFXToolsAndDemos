package de.mpmediasoft.jfxtools.canvas;

import java.nio.ByteBuffer;

/**
 * The JNI interface to the native renderer.
 * 
 * @author Michael Paus
 */
public class NativeRenderer {
    
    static {
        System.loadLibrary("nativerenderer");
    }
    
    // Initialization and disposal:
    
    public native void init();
        
    public native void dispose();
    
    // Canvas creation and rendering:
    
    public native ByteBuffer createCanvas(int width, int height, int numBuffers, int nativeColorModel);
        
    public native int render();
    
    // Actions, e.g., due to user input events:
        
    public native void moveTo(int x, int y);
    
    // TODO: zoom, rotate, ...
    
}
