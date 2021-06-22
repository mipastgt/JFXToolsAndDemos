package de.mpmediasoft.jfxtools.skiafx;

import java.lang.reflect.Field;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.jetbrains.skija.Canvas;
import org.jetbrains.skija.ImageInfo;
import org.jetbrains.skija.Surface;

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
 * @author mpaus
 */
public class SkiaSurfaceFX {
    
    // Uses the "Foreign Memory Access API" introduced in Java 14.
    // Needs --add-modules=jdk.incubator.foreign on the command line if set to true!
//    private final static boolean AVOID_ILLEGAL_REFLECTION = false;
    
    public static interface RenderCallback extends Callback<Canvas,Rectangle2D> {}
    
    private final ByteBuffer byteBuffer;
    
    private final PixelBuffer<IntBuffer> pixelBuffer;
    
    private final Image image;
    
    private final Surface surface;
    
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
    
    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public PixelBuffer<IntBuffer> getPixelBuffer() {
        return pixelBuffer;
    }

    public Image getImage() {
        return image;
    }
    
    public Surface getSurface() {
        return surface;
    }
    
    public void render(RenderCallback renderer) {
        pixelBuffer.updateBuffer(pb -> renderer.call(surface.getCanvas()));
    }
    
    private final long getBufferPointer(ByteBuffer byteBuffer) throws Exception {
//        if (AVOID_ILLEGAL_REFLECTION) {
//            return MemorySegment.ofByteBuffer(byteBuffer).baseAddress().toRawLongValue();
//        } else {        
            Field address = Buffer.class.getDeclaredField("address");
            address.setAccessible(true);
            return address.getLong(byteBuffer);
//        }
    }
    
}
