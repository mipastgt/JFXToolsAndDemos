package de.mpmediasoft.jfxtools.canvas;

import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
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
 * - Handle different render scales.
 * - Packaging of native part into jar file.
 * 
 * @author Michael Paus
 */
public class NativeRenderingCanvas {
    
    // Configure this to use double-buffering [2] or not [1].
    private final int numBuffers = 2;
    
    // Configure this to use an external thread or the JavaFX application thread for rendering.
    private final boolean useRenderingService = true;
    
    private final int MAX_THREADS = 1; // More than one thread does not make sense for this service setup!
    
    private int threadCounter = 0;

    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS, runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.setName("NativeRenderer_" + (threadCounter++));
        return t ;
    });

    private final PixelFormat<IntBuffer> pixelFormat;
    private final ObjectProperty<WritableImage> fxImage;    
    private final ImageView imageView;
    private final Pane canvasPane;
    private final NativeRenderer nativeRenderer;
    private final RenderingService renderingService;
    
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
        renderingService = new RenderingService();
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
                nrViewX += mx - e.getX();
                nrViewY += my - e.getY();            
                mx = e.getX();
                my = e.getY();
                e.consume();
                
                render();
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
            e.consume();
            
            render();
        });
        
        imageView.setOnZoom(e -> {
//            System.out.println("setOnZoom: " + e.getZoomFactor());
            // Action not yet implemented.
            e.consume();
            
            render();
        });
        
        imageView.setOnRotate(e -> {
//            System.out.println("setOnZoom: " + e.getAngle() + " " + e.getTotalAngle());
            // Action not yet implemented.
            e.consume();
            
            render();
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
	        if (useRenderingService) {
	            renderingService.renderIfIdle();
	        } else {
                final int bufferIndex = renderAction();
                renderUpdate(bufferIndex);
	        }
	    }
	}
	
	// Can be called on any thread.
    private int renderAction() {
//        System.out.println("Rendering on: " + Thread.currentThread().getName());
        nativeRenderer.moveTo(nrViewX, nrViewY);
        return nativeRenderer.render();
    }
    
    // Must be called on JavaFX application thread.
    private void renderUpdate(int bufferIndex) {
        assert Platform.isFxApplicationThread() : "Not called on JavaFX application thread.";
        pixelBuffer.updateBuffer(pb -> {
            final Rectangle2D renderedFrame = new Rectangle2D(0.0, bufferIndex * nrViewHeight, imageView.getFitWidth(), imageView.getFitHeight());
            imageView.setViewport(renderedFrame);
            return renderedFrame;
        });
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
    
    private class RenderingService extends Service<Integer> {        
        RenderingService() {
            setExecutor(executorService);
            
            this.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    renderUpdate((Integer)t.getSource().getValue());
                }
            });
        }
 
        void renderIfIdle() {
            State state = getState();
            if (state != State.SCHEDULED && state != State.RUNNING) {
                restart();
            }
        }
        
        @Override
        protected Task<Integer> createTask() {
            return new Task<Integer>() {
                @Override
                protected Integer call() {
                    return renderAction();
                }
            };
        }
    }
    
}
