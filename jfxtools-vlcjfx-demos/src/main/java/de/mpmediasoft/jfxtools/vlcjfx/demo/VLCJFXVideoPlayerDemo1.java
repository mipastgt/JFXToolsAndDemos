package de.mpmediasoft.jfxtools.vlcjfx.demo;

import de.mpmediasoft.jfxtools.vlcjfx.VLCJFXVideoPlayer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * A minimal demo program to show how the VLCJFXVideoPlayer class is supposed to be used.
 * 
 * This should work on macOS and Linux but there currently seem to be problems with the VLC code
 * on Windows.
 * 
 * In order to run the code, a recent version of the VLC player (3.0.x+) must be installed
 * on the system. Other dependencies can be found in the pom.xml.
 * 
 * @author Michael Paus
 */
public class VLCJFXVideoPlayerDemo1 extends Application {

    private static final String VIDEO_FILE =
//        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
        "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4";
//        "http://ftp.nluug.nl/pub/graphics/blender/demo/movies/ToS/tearsofsteel_4k.mov";
        
    private final double WIDTH = 1200;
    private final double HEIGHT = 675;
    
    private VLCJFXVideoPlayer videoPlayer;
        
    private ImageView videoImageView;

    @Override
    public void init() {
        System.out.println("java.runtime.version: " + System.getProperty("java.runtime.version", "(undefined)"));
        System.out.println("javafx.runtime.version: " + System.getProperty("javafx.runtime.version", "(undefined)"));
    }
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        videoPlayer = new VLCJFXVideoPlayer();
        
        StackPane root = new StackPane();
        root.getStyleClass().add("videoPane");
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.fitWidthProperty().bind(root.widthProperty()); 
        videoImageView.fitHeightProperty().bind(root.heightProperty()); 
        videoImageView.imageProperty().bind(videoPlayer.videoImageProperty());
        root.getChildren().add(videoImageView);
        
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add("/video_player.css");
        primaryStage.setScene(scene);
        primaryStage.show();

        videoPlayer.mediaResourceLocatorProperty().set(VIDEO_FILE);
    }

    @Override
    public void stop() throws Exception {
        videoPlayer.dispose();
    }
    
    public static void main(String[] args) {
        launch(args);
    }

}

//Launch via this class to avoid module system headaches.
class VLCJFXVideoPlayerDemo1Launcher {public static void main(String[] args) {VLCJFXVideoPlayerDemo1.main(args);}}
