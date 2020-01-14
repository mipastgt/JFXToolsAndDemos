package de.mpmediasoft.jfxtools.canvas;

import java.nio.IntBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
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
    private final boolean doRenderingAsynchronous = false; // The resizing does not work perfectly yet !!!
    
    private final int MAX_THREADS = 1; // More than one thread does not make sense for this service setup!
    
    private final ExecutorService executorService = Executors.newFixedThreadPool(MAX_THREADS, runnable -> {
        Thread t = new Thread(runnable);
        t.setDaemon(true);
        t.setName("NativeRenderer");
        return t ;
    });

    private final PixelFormat<IntBuffer> pixelFormat;
    private final ObjectProperty<WritableImage> fxImage;    
    private final ImageView imageView;
    private final Pane canvasPane;
    private final NativeRenderer nativeRenderer;
    private final RenderingService renderingService;
    private final ChangeListener<? super Bounds> resizeListener;
    
    private PixelBuffer<IntBuffer> pixelBuffer;
    
    // The native renderer view size. Its width and height are multiples of nrViewIncrement
    // and thus will normally be larger than the canvasPane width and height.
    private int nrViewIncrement = 64; 
    private final Viewport emptyViewport = new Viewport();
    private Viewport nrViewport = emptyViewport;
    
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
	            
        canvasPane.getChildren().add(imageView);
        
        resizeListener = (v,o,n) -> {
            render(nrViewport.withSizeIncrement((int)canvasPane.getWidth(), (int)canvasPane.getHeight(), nrViewIncrement));
        };
        
        init();
	}
	
    /**
     * Must be called before the NativeRenderingCanvas can be used again after dispose() has been called.
     */
    public void init() {                
        canvasPane.boundsInLocalProperty().addListener(resizeListener);
        
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
                Viewport newViewport = nrViewport.withDeltaLocation((int)(mx - e.getX()), (int)(my - e.getY()));
                mx = e.getX();
                my = e.getY();
                e.consume();
                
                render(newViewport);
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
            
            
            Viewport newViewport = nrViewport;
            if (scrollAction == ScrollAction.ZOOM) {
//                System.out.print("Zoom: ");
                // Action not yet implemented.
            } else {
//                System.out.print("Scroll: ");
                newViewport = nrViewport.withDeltaLocation((int)-e.getDeltaX(), (int)-e.getDeltaY());
            }
//            System.out.print(e.getDeltaX() + " " + e.getTotalDeltaX() + " " + e.getDeltaY() + " " + e.getTotalDeltaY() + " " + e.isInertia());
//            System.out.print(" " + e.getMultiplierX() + " " + e.getMultiplierY() + " " + e.getTextDeltaX() + " " + e.getTextDeltaY());
//            System.out.print(" " + e.getTextDeltaXUnits() + " " + e.getTextDeltaYUnits());
//            System.out.print(" " + e.getX() + " " + e.getY());
//            System.out.println();
            e.consume();
            
            render(newViewport);
        });
        
        imageView.setOnZoom(e -> {
//            System.out.println("setOnZoom: " + e.getZoomFactor());
            // Action not yet implemented.
            Viewport newViewport = nrViewport;
            e.consume();
            
            render(newViewport);
        });
        
        imageView.setOnRotate(e -> {
//            System.out.println("setOnZoom: " + e.getAngle() + " " + e.getTotalAngle());
            // Action not yet implemented.
            Viewport newViewport = nrViewport;
            e.consume();
            
            render(newViewport);
        });        
    }
        
    /**
     * Dispose all resources and disable all actions. Init() has to be called
     * before the NativeRenderingCanvas instance can be used again.
     */
    public void dispose() {
        nrViewport = emptyViewport;
        inScrollBrackets = false;
        
        canvasPane.boundsInLocalProperty().removeListener(resizeListener);
        
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
	
	private void render(Viewport viewport) {
	    if (doRenderingAsynchronous) {
            nrViewport = viewport;
            renderingService.renderIfIdle(viewport);
        } else {
            checkCanvas(viewport, nrViewport);                        
            nrViewport = viewport;
            if (pixelBuffer != null) {
                renderUpdate(renderAction(viewport), viewport);
            }
        }
	}
	
	private void checkCanvas(Viewport newViewport, Viewport oldViewport) {
        if (newViewport != oldViewport) {
            if (newViewport.getWidth() != oldViewport.getWidth() || newViewport.getHeight() != oldViewport.getHeight()) {
                final IntBuffer intBuffer = nativeRenderer.createCanvas(newViewport.getWidth(), newViewport.getHeight(), numBuffers, NativeColorModel.INT_ARGB_PRE.ordinal()).asIntBuffer();        
                pixelBuffer = new PixelBuffer<>(newViewport.getWidth(), numBuffers * newViewport.getHeight(), intBuffer, pixelFormat);                
                fxImage.set(new WritableImage(pixelBuffer));
            }
        }
	}
	
	// Can be called on any thread.
    private int renderAction(Viewport viewport) {
//        System.out.println("Rendering on: " + Thread.currentThread().getName());
        nativeRenderer.moveTo(viewport.getMinX(), viewport.getMinY());
        return nativeRenderer.render();
    }
    
    // Must be called on JavaFX application thread.
    private void renderUpdate(int bufferIndex, Viewport viewport) {
        assert Platform.isFxApplicationThread() : "Not called on JavaFX application thread.";
        pixelBuffer.updateBuffer(pb -> {
            final Rectangle2D renderedFrame = new Rectangle2D(
                0,
                bufferIndex * viewport.getHeight(),
                Math.min(canvasPane.getWidth(), viewport.getWidth()),
                Math.min(canvasPane.getHeight(), viewport.getHeight()));            
            imageView.setViewport(renderedFrame);
            return renderedFrame;
        });
    }
    
    private class RenderingService extends Service<Integer> {
        private Viewport oldViewport = emptyViewport;
        private Viewport newViewport = emptyViewport;
        private Viewport dirtyViewport = emptyViewport;

        RenderingService() {
            setExecutor(executorService);

            this.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent t) {
                    renderUpdate((Integer) t.getSource().getValue(), newViewport);
                    renderIfIdle(dirtyViewport);
                }
            });
        }

        void renderIfIdle(Viewport viewport) {
            assert Platform.isFxApplicationThread() : "Not called on JavaFX application thread.";
            if(! viewport.isEmpty()) {
                dirtyViewport = viewport;
                State state = getState();
                if (state != State.SCHEDULED && state != State.RUNNING) {
                    restart();
                }
            }
        }

        @Override
        protected Task<Integer> createTask() {
            return new Task<Integer>() {
                @Override
                protected Integer call() {
                    oldViewport = newViewport;
                    newViewport = dirtyViewport;
                    dirtyViewport = emptyViewport;
                    checkCanvas(newViewport, oldViewport);            
                    return renderAction(newViewport);
                }
            };
        }
    }
    
}
