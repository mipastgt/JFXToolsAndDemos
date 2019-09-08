package de.mpmediasoft.jfxtools.vlcjfx;

import java.nio.ByteBuffer;

import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.image.Image;
import javafx.scene.image.PixelBuffer;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import uk.co.caprica.vlcj.factory.MediaPlayerFactory;
import uk.co.caprica.vlcj.player.base.ControlsApi;
import uk.co.caprica.vlcj.player.base.MediaPlayer;
import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;
import uk.co.caprica.vlcj.player.embedded.videosurface.CallbackVideoSurface;
import uk.co.caprica.vlcj.player.embedded.videosurface.VideoSurfaceAdapters;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormat;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.BufferFormatCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.RenderCallback;
import uk.co.caprica.vlcj.player.embedded.videosurface.callback.format.RV32BufferFormat;

/**
 * This is a very simple example of a video player which uses the new WritableImage
 * of JavaFX 13 with support for Buffers. The idea is to let VLC directly render into
 * this buffer and use the image directly in an ImageView without any explicit rendering
 * into a canvas or such thing. Only this brings the desired performance boost.
 * 
 * What I have not considered yet is any kind of synchronization.
 * I think an extension of the PixelBuffer to support some kine of double-buffering
 * would be the right thing to do.
 * 
 * This should work on macOS and Linux but there currently seem to be problems with the VLC code
 * on Windows.
 * 
 * In order to run the code, a recent version of the VLC player (3.0.x+) must be installed
 * on the system. Other dependencies can be found in the pom.xml.
 * 
 * Tested on macOS 10.14.6 and Linux.
 * 
 * @author Michael Paus
 */
public class VLCJFXVideoPlayer {
    
    private final MediaPlayerFactory mediaPlayerFactory;

    private final EmbeddedMediaPlayer embeddedMediaPlayer;
    
    private PixelBuffer<ByteBuffer> videoPixelBuffer;
    
    private final StringProperty mediaResourceLocator = new SimpleStringProperty(null);
    public StringProperty mediaResourceLocatorProperty() {return mediaResourceLocator;};

    private final BooleanProperty autoPlay = new SimpleBooleanProperty(true);
    public BooleanProperty autoPlayProperty() {return autoPlay;};

    private final ReadOnlyObjectWrapper<Image> videoImage = new ReadOnlyObjectWrapper<>();
    public ReadOnlyObjectProperty<Image> videoImageProperty() {return videoImage.getReadOnlyProperty();};
    
    private final ReadOnlyBooleanWrapper error = new ReadOnlyBooleanWrapper(false);
    public ReadOnlyBooleanProperty errorProperty() {return error.getReadOnlyProperty();};

    /**
     * The constructor to create the video player.
     */
    public VLCJFXVideoPlayer() {
        mediaPlayerFactory = new MediaPlayerFactory();
        embeddedMediaPlayer = mediaPlayerFactory.mediaPlayers().newEmbeddedMediaPlayer();
        embeddedMediaPlayer.videoSurface().set(new FXCallbackVideoSurface());
        
        mediaResourceLocator.addListener((v,o,n) -> {
            if (autoPlay.get()) {
                error.set(embeddedMediaPlayer.media().play(n));
            } else {
                error.set(embeddedMediaPlayer.media().prepare(n));
            }
        });
    }
    
    /**
     * Get access to the media player controls.
     * 
     * @return the media player controls.
     */
    public ControlsApi controls() {
        return embeddedMediaPlayer.controls();
    }
    
    /**
     * Dispose the media player resources.
     */
    public void dispose() {
        embeddedMediaPlayer.controls().stop();
        embeddedMediaPlayer.release();
        mediaPlayerFactory.release();
    }

    private class FXCallbackVideoSurface extends CallbackVideoSurface {
        FXCallbackVideoSurface() {
            super(new FXBufferFormatCallback(), new FXRenderCallback(), true, VideoSurfaceAdapters.getVideoSurfaceAdapter());
        }
    }

    private class FXBufferFormatCallback implements BufferFormatCallback {
        private int sourceWidth;
        private int sourceHeight;
        
        @Override
        public BufferFormat getBufferFormat(int sourceWidth, int sourceHeight) {
            this.sourceWidth = sourceWidth;
            this.sourceHeight = sourceHeight;           
            return new RV32BufferFormat(sourceWidth, sourceHeight);
        }

        @Override
        public void allocatedBuffers(ByteBuffer[] buffers) {
            assert buffers.length == 1;
            assert buffers[0].capacity() == sourceWidth * sourceHeight * 4;
            PixelFormat<ByteBuffer> pixelFormat = PixelFormat.getByteBgraPreInstance();
            videoPixelBuffer = new PixelBuffer<>(sourceWidth, sourceHeight, buffers[0], pixelFormat);
            videoImage.set(new WritableImage(videoPixelBuffer));
        }
    }

    private class FXRenderCallback implements RenderCallback {
        @Override
        public void display(MediaPlayer mediaPlayer, ByteBuffer[] nativeBuffers, BufferFormat bufferFormat) {
            Platform.runLater(() -> {
                videoPixelBuffer.updateBuffer(pb -> {
                    return null;
                });
            });
        }
    }
    
}
