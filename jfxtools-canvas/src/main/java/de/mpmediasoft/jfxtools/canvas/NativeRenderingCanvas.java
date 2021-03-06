package de.mpmediasoft.jfxtools.canvas;

import java.nio.ByteBuffer;
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
 * - Implement further user actions.
 * - Handle different render scales.
 * - Packaging of native part into jar file.
 * 
 * @author Michael Paus
 */
public class NativeRenderingCanvas {
    
    // Configure this to use double-buffering [2] or not [1].
    private final int numBuffers = 2;
    
    // Configure this to use an external thread or the JavaFX application thread for rendering.
    private final boolean doRenderingAsynchronously = false; // The resizing does not work perfectly yet !!!
    
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
    
    private ByteBuffer oldRawByteBuffer;
    private ByteBuffer newRawByteBuffer;
    private PixelBuffer<IntBuffer> pixelBuffer;
    
    // The native renderer viewport. Its width and height are multiples of nrViewIncrement
    // and thus will normally be larger than the canvasPanes width and height.
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
                mx = e.getX();
                my = e.getY();            
                e.consume();
            }
        });
        
        imageView.setOnMouseReleased(e -> {
            if (! e.isSynthesized()) {
                mx = 0.0;
                my = 0.0;            
                e.consume();
            }
        });
        
        imageView.setOnMouseDragged(e -> {
            if (! e.isSynthesized()) {
                Viewport newViewport = nrViewport.withDeltaLocation((int)(mx - e.getX()), (int)(my - e.getY()));
                mx = e.getX();
                my = e.getY();
                e.consume();
                
                render(newViewport);
            }
        });
        
        imageView.setOnScrollStarted(e -> {
            inScrollBrackets = true;
        });
        
        imageView.setOnScrollFinished(e -> {
            inScrollBrackets = false;
        });
        
        imageView.setOnScroll(e -> {
            
            // According to the JavaFX documentation, scroll started/finished indicates that this gesture was
            // performed on a touch device and not the mouse wheel. But due to a bug (at least on macOS, see:
            // https://bugs.openjdk.java.net/browse/JDK-8236971 ) this mechanism currently does not work            
            // for JDKs above 11 independent of the JFX version used.
            
            // This simple mechanism does not work due to above bug because the total-delta values are NOT zero for mouse-wheels.            
//          ScrollAction scrollAction = (e.getTotalDeltaX() != 0 || e.getTotalDeltaY() != 0.0) ? ScrollAction.ZOOM : ScrollAction.PAN;
            
            // We need all these criteria to find out whether this event comes from a mouse wheel
            // and it remains to be tested whether this works on all platforms and all devices.
            // Also this workarround only works with JDKs <= 11.
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
                        
            Viewport newViewport;
            if (scrollAction == ScrollAction.ZOOM) {
                // TODO: Implement action.
                newViewport = nrViewport;
            } else {
                newViewport = nrViewport.withDeltaLocation((int)-e.getDeltaX(), (int)-e.getDeltaY());
            }
            e.consume();
            
            render(newViewport);
        });
        
        imageView.setOnZoom(e -> {
            // TODO: Implement action.
            Viewport newViewport = nrViewport;
            e.consume();
            
            render(newViewport);
        });
        
        imageView.setOnRotate(e -> {
            // TODO: Implement action.
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
	    if (! viewport.isEmpty()) {
    	    if (doRenderingAsynchronously) {
                renderingService.renderIfIdle(viewport);
            } else {
                renderUpdate(renderAction(viewport, nrViewport), viewport);
            }
            nrViewport = viewport;
	    }
	}
	
	// Can be called on any thread.
    private int renderAction(Viewport newViewport, Viewport oldViewport) {
        if (newViewport != oldViewport) {
            if (newViewport.getWidth() != oldViewport.getWidth() || newViewport.getHeight() != oldViewport.getHeight()) {
                newRawByteBuffer = nativeRenderer.createCanvas(newViewport.getWidth(), newViewport.getHeight(), numBuffers, NativeColorModel.INT_ARGB_PRE.ordinal());        
            }
        }
        nativeRenderer.moveTo(newViewport.getMinX(), newViewport.getMinY());
        return nativeRenderer.render();
    }
    
    // Must be called on JavaFX application thread.
    private void renderUpdate(int bufferIndex, Viewport viewport) {
        assert Platform.isFxApplicationThread() : "Not called on JavaFX application thread.";
        if (newRawByteBuffer != oldRawByteBuffer) {
            oldRawByteBuffer = newRawByteBuffer;
            final IntBuffer intBuffer = newRawByteBuffer.asIntBuffer();
            pixelBuffer = new PixelBuffer<>(viewport.getWidth(), numBuffers * viewport.getHeight(), intBuffer, pixelFormat);
            fxImage.set(new WritableImage(pixelBuffer));
        }
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
                    return renderAction(newViewport, oldViewport);
                }
            };
        }
    }
    
}
