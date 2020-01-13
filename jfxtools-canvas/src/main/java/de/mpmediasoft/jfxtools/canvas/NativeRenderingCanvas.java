package de.mpmediasoft.jfxtools.canvas;

import java.nio.IntBuffer;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

/**
 * A native rendering canvas. The assumption is that some native renderer
 * produces an image provided as an IntBuffer or ByteBuffer. The PixelFormats
 * must be IntArgbPre or ByteBgraPre respectively. For the API see NativeRenderer.
 * 
 * This buffer is then used to create an Image which is bound to an ImageView.
 * This class manages the direct display of this Image in a Pane and reacts to
 * user input via mouse input or gestures on touch devices.
 * 
 * TODOs:
 * - Run the native renderer on a separate thread.
 * - Handle different render scales.
 * - Packaging of native part into jar file.
 * 
 * @author Michael Paus
 */
public class NativeRenderingCanvas {
    
    // Configure this to use double-buffering [2] or not [1].
    private int numBuffers = 2;
    
    private final PixelFormat<IntBuffer> pixelFormat;
    private final ObjectProperty<WritableImage> fxImage;    
    private final ImageView imageView;
    private final Pane canvasPane;
    private final NativeRenderer nativeRenderer;
    
    private PixelBuffer<IntBuffer> pixelBuffer;
    
    // The native renderer view size. Its width and height are multiples of nrViewIncrement
    // and thus will normally be larger than the canvasPane width and height.
    private int nrViewIncrement = 64;    
    private int nrViewX = 0;
    private int nrViewY = 0;
    private int nrViewWidth = 0;
    private int nrViewHeight = 0;
    
    private double mx = 0.0;
    private double my = 0.0;
    
    private boolean inScrollBrackets = false;
    
    /**
     * Create and initialize a NativeRenderingCanvas instance.
     */
    public NativeRenderingCanvas() {
        nativeRenderer = new NativeRenderer();
        canvasPane = new Pane();
        fxImage = new SimpleObjectProperty<>();
        pixelFormat = PixelFormat.getIntArgbPreInstance();
        
	    imageView = new ImageView();
	    imageView.imageProperty().bind(fxImage);
        imageView.fitWidthProperty().bind(canvasPane.widthProperty());
        imageView.fitHeightProperty().bind(canvasPane.heightProperty());        
	    imageView.setManaged(false); // !!!
	    imageView.setPreserveRatio(true);
	    imageView.setPickOnBounds(true);
	            
	    canvasPane.boundsInLocalProperty().addListener((v,o,n) -> {
            createCanvas((int)imageView.getFitWidth(), (int)imageView.getFitHeight());      
            render();
        });             
        canvasPane.getChildren().add(imageView);
        
        init();
	}
	
    /**
     * Must be called before the NativeRenderingCanvas can be used again after dispose() has been called.
     */
    public void init() {                
        imageView.setOnMousePressed(e -> {
            if (! e.isSynthesized()) {
//                System.out.println("setOnMousePressed");
                mx = e.getX();
                my = e.getY();            
                e.consume();
            }
        });
        
        imageView.setOnMouseReleased(e -> {
            if (! e.isSynthesized()) {
//                System.out.println("setOnMouseReleased");
                mx = 0.0;
                my = 0.0;            
                e.consume();
            }
        });
        
        imageView.setOnMouseDragged(e -> {
            if (! e.isSynthesized()) {
//                System.out.println("setOnMouseDragged");
                double dx = mx - e.getX();
                double dy = my - e.getY();
                nrViewX += dx;
                nrViewY += dy;            
                render();
                mx = e.getX();
                my = e.getY();
                e.consume();
            }
        });
        
        // According to the JavaFX documentation, scroll started/finished indicates that this gesture was
        // performed on a touch device and not the mouse wheel. But due to a bug (at least on macOS, see:
        // https://bugs.openjdk.java.net/browse/JDK-8236971 ) this mechanism currently does not work.
        
        // It only works for JDKs up to 11 but JFX can be the latest version.
        
        imageView.setOnScrollStarted(e -> {
//            System.out.println("setOnScrollStarted");
            inScrollBrackets = true;
        });
        
        imageView.setOnScrollFinished(e -> {
//            System.out.println("setOnScrollFinished");
            inScrollBrackets = false;
        });
        
        imageView.setOnScroll(e -> {
            
            // This mechanism does not work due to above bug because the total-delta values are NOT zero for mouse-wheels.            
//          ScrollAction scrollAction = (e.getTotalDeltaX() != 0 || e.getTotalDeltaY() != 0.0) ? ScrollAction.ZOOM : ScrollAction.PAN;
            
            // An ugly fix is to force the user to make the distinction by pressing the shortcut-key to indicate a zoom.
//          ScrollAction scrollAction = (e.isShortcutDown()) ? ScrollAction.ZOOM : ScrollAction.PAN;
            
            // We need all these criteria to find out whether this event comes from a mouse wheel
            // and it remains to be tested whether this works on all platforms and all devices.
            ScrollAction scrollAction;
            if (! inScrollBrackets &&
                ! e.isInertia() &&
                Math.abs(e.getDeltaX()) == 0.0 &&
                e.getDeltaY() == e.getTotalDeltaY() &&
                Math.abs(e.getDeltaY()) > 1.0000001)
            {
                scrollAction = ScrollAction.ZOOM;
            } else {
                scrollAction = ScrollAction.PAN;
            }
            
            if (scrollAction == ScrollAction.ZOOM) {
//                System.out.print("Zoom: ");
                // Action not yet implemented.
            } else {
//                System.out.print("Scroll: ");
                nrViewX -= e.getDeltaX();
                nrViewY -= e.getDeltaY();
            }
//            System.out.print(e.getDeltaX() + " " + e.getTotalDeltaX() + " " + e.getDeltaY() + " " + e.getTotalDeltaY() + " " + e.isInertia());
//            System.out.print(" " + e.getMultiplierX() + " " + e.getMultiplierY() + " " + e.getTextDeltaX() + " " + e.getTextDeltaY());
//            System.out.print(" " + e.getTextDeltaXUnits() + " " + e.getTextDeltaYUnits());
//            System.out.print(" " + e.getX() + " " + e.getY());
//            System.out.println();
            render();
            e.consume();
        });
        
        imageView.setOnZoom(e -> {
//            System.out.println("setOnZoom: " + e.getZoomFactor());
            // Action not yet implemented.
            render();
            e.consume();
        });
        
        imageView.setOnRotate(e -> {
//            System.out.println("setOnZoom: " + e.getAngle() + " " + e.getTotalAngle());
            // Action not yet implemented.
            render();
            e.consume();
        });        
    }
        
    /**
     * Dispose all resources and disable all actions. Init() has to be called
     * before the NativeRenderingCanvas instance can be used again.
     */
    public void dispose() {
        nrViewX = 0;
        nrViewY = 0;
        nrViewWidth = 0;
        nrViewHeight = 0;        
        inScrollBrackets = false;
        
        imageView.setOnMouseClicked(null);        
        imageView.setOnMousePressed(null);        
        imageView.setOnMouseReleased(null);        
        imageView.setOnMouseDragged(null);
        imageView.setOnScrollStarted(null);
        imageView.setOnScrollFinished(null);
        imageView.setOnScroll(null);
        imageView.setOnZoom(null);
        imageView.setOnRotate(null);

        fxImage.set(null);
        
        nativeRenderer.dispose();
    }
    
	/**
	 * Return the root node of the NativeRenderingCanvas which can be directly
	 * added to some layout-pane.
	 * 
	 * @return the root node of the NativeRenderingCanvas.
	 */
	public Node getRoot() {return canvasPane;}
	
	private void render() {
	    if (pixelBuffer != null) {
	        pixelBuffer.updateBuffer(pb -> {
//                long t0 = System.nanoTime();
                nativeRenderer.moveTo(nrViewX, nrViewY);
                int bufferIndex = nativeRenderer.render();
                final Rectangle2D renderedFrame = new Rectangle2D(0.0, bufferIndex * nrViewHeight, imageView.getFitWidth(), imageView.getFitHeight());
                imageView.setViewport(renderedFrame);
//                long t1 = System.nanoTime();
//                System.out.format("Rendering: %f millis\n", (t1 - t0)*0.000_001);
                return renderedFrame;
	        });
	    }
	}
	
    private void createCanvas(int width, int height) {
        if (width > 0 && height > 0) {
            // Increment or decrement the view size in steps of view_incr.
            int newNrViewWidth = (width % nrViewIncrement > 0) ? (width / nrViewIncrement + 1) * nrViewIncrement : (width / nrViewIncrement) * nrViewIncrement;
            int newNrViewHeight = (height % nrViewIncrement > 0) ? (height / nrViewIncrement + 1) * nrViewIncrement : (height / nrViewIncrement) * nrViewIncrement;
            
//            System.out.println(width + " " + view_width + " " + new_view_width + " : " + height + " " + view_height + " " + new_view_height);            
            
            if (newNrViewWidth != nrViewWidth || newNrViewHeight != nrViewHeight) {
                nrViewWidth = newNrViewWidth;
                nrViewHeight = newNrViewHeight;
                        
                final IntBuffer intBuffer = nativeRenderer.createCanvas(nrViewWidth, nrViewHeight, numBuffers, NativeColorModel.INT_ARGB_PRE.ordinal()).asIntBuffer();        
                pixelBuffer = new PixelBuffer<>(nrViewWidth, numBuffers * nrViewHeight, intBuffer, pixelFormat);
                
                fxImage.set(new WritableImage(pixelBuffer));
            }
        }
    }
    
}
