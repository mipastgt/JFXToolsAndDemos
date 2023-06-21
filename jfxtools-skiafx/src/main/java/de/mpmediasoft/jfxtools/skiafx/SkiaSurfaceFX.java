package de.mpmediasoft.jfxtools.skiafx;

import java.lang.foreign.MemorySegment;
import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;

import javafx.geometry.Rectangle2D;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.util.Callback;
//import jdk.incubator.foreign.MemorySegment;

/**
 * A JavaFX wrapper class for a Skia Surface.
 * You can draw into the Skia Canvas of it via the render method.
 * The rendering result is made directly available in an image via
 * a PixelBuffer.
 * 
 * @author Michael Paus
 */
public class SkiaSurfaceFX {
    
    // Uses the "Foreign Memory Access API" introduced in Java 14.
    // Needs --add-modules=jdk.incubator.foreign on the command line if set to true!
    private final static boolean AVOID_ILLEGAL_REFLECTION = true;
    
    public static interface RenderCallback extends Callback<Canvas,Rectangle2D> {}
    
    private final ByteBuffer byteBuffer;
    
    private final PixelBuffer<IntBuffer> pixelBuffer;
    
    private final Image image;
    
    private final Surface surface;
    
    /**
     * Generates a raster Skia surface.<br>
     * Note: Skia does not do any automatic scaling for Retina or HighDPI screens
     * like the JavaFX canvas does. The user has to take care of that manually.
     * 
     * @param width the width in pixels.
     * @param height the height in pixels.
     */
    public SkiaSurfaceFX(int width, int height) {
        try {
            byteBuffer = ByteBuffer.allocateDirect(width * height * 4);
            pixelBuffer = new PixelBuffer<>(width, height, byteBuffer.asIntBuffer(), PixelFormat.getIntArgbPreInstance());
            image = new WritableImage(pixelBuffer);
            surface = Surface.makeRasterDirect(ImageInfo.makeN32Premul(width, height), getBufferPointer(byteBuffer), width * 4);
        } catch (Exception e) {
            throw new RuntimeException("Creation of Skia surface failed.", e);
        }
    }
    
    /**
     * Get the byte buffer if you need it.
     * 
     * @return the byte buffer.
     */
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    /**
     * Get the pixel buffer if you need it.
     * 
     * @return the pixel buffer.
     */
    public PixelBuffer<IntBuffer> getPixelBuffer() {
        return pixelBuffer;
    }

    /**
     * Get the image the Canvas is rendering to.
     * 
     * @return the image.
     */
    public Image getImage() {
        return image;
    }
    
    /**
     * Get the Skija Surface if you need it.
     * 
     * @return the Skija Surface.
     */
    public Surface getSurface() {
        return surface;
    }
    
    /**
     * Render something into the Canvas of this Surface.
     * You have to provide a Callback which takes a Canvas and returns a JavaFX
     * Rectangle2D or null. See the PixelBuffer.updateBuffer method for more details.
     * 
     * @param renderer the renderer Callback.
     */
    public void render(RenderCallback renderer) {
        pixelBuffer.updateBuffer(pb -> renderer.call(surface.getCanvas()));
    }
    
    private final long getBufferPointer(ByteBuffer byteBuffer) throws Exception {
        if (AVOID_ILLEGAL_REFLECTION) {
            return MemorySegment.ofBuffer(byteBuffer).address().toRawLongValue();
        } else {        
            Field address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            return address.getLong(byteBuffer);
        }
    }
    
}
