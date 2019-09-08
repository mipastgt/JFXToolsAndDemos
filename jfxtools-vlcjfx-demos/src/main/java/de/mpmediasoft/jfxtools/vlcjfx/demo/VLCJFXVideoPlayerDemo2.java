package de.mpmediasoft.jfxtools.vlcjfx.demo;

import java.util.Map;

import de.mpmediasoft.jfxtools.vlcjfx.VLCJFXVideoPlayer;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * An extended demo program to show how the VLCJFXVideoPlayer class is supposed to be used.
 * It provides some controls to select a video and play, pause and stop it as an overlay
 * on top of the video.
 * 
 * This should work on macOS and Linux but there currently seem to be problems with the VLC code
 * on Windows.
 * 
 * In order to run the code, a recent version of the VLC player (3.0.x+) must be installed
 * on the system. Other dependencies can be found in the pom.xml.
 * 
 * @author Michael Paus
 */
public class VLCJFXVideoPlayerDemo2 extends Application {

    private final Map<String, String> videoMap = Map.of(
        "BigBuckBunny", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
        "ElephantsDream", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
        "Sintel", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4",
        "TearsOfSteel", "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4"
        );
        
    private final ComboBox<String> videoSelectionComboBox = new ComboBox<>(FXCollections.observableArrayList(videoMap.keySet()));

    private final double WIDTH = 1200;
    private final double HEIGHT = 675;
    
    private VLCJFXVideoPlayer videoPlayer;
        
    private ImageView videoImageView;

    @Override
    public void start(Stage primaryStage) throws Exception {
        videoPlayer = new VLCJFXVideoPlayer();
        videoPlayer.autoPlayProperty().set(true);
        
        StackPane root = new StackPane();
        root.getStyleClass().add("videoPane");
        
        videoImageView = new ImageView();
        videoImageView.setPreserveRatio(true);
        videoImageView.fitWidthProperty().bind(root.widthProperty()); 
        videoImageView.fitHeightProperty().bind(root.heightProperty()); 
        videoImageView.imageProperty().bind(videoPlayer.videoImageProperty());
        root.getChildren().add(videoImageView);
        
        AnchorPane controlsPane = new AnchorPane();
        root.getChildren().add(controlsPane);
        
        videoSelectionComboBox.getSelectionModel().select(0);
        videoSelectionComboBox.setOnAction(ev -> updateMediaResource());
        
        Button playButton = new Button("Play");
        playButton.setOnAction(ev -> videoPlayer.controls().play());
        
        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(ev -> videoPlayer.controls().setPause(true));
        
        Button stopButton = new Button("Stop");
        stopButton.setOnAction(ev -> videoPlayer.controls().stop());
        
        HBox controls = new HBox();
        controls.setAlignment(Pos.CENTER);
        controls.setSpacing(20);
        controls.getChildren().addAll(videoSelectionComboBox, playButton, pauseButton, stopButton);

        AnchorPane.setLeftAnchor(controls, 20.0);
        AnchorPane.setBottomAnchor(controls, 20.0);
        AnchorPane.setRightAnchor(controls, 20.0);
        
        controlsPane.getChildren().add(controls);
                
        Scene scene = new Scene(root, WIDTH, HEIGHT);
        scene.getStylesheets().add("/video_player.css");
        primaryStage.setScene(scene);
        primaryStage.show();

        updateMediaResource();
    }

    @Override
    public void stop() throws Exception {
        videoPlayer.dispose();
    }
    
    private void updateMediaResource() {
        String mediaResource = videoMap.get(videoSelectionComboBox.getSelectionModel().getSelectedItem());
        videoPlayer.mediaResourceLocatorProperty().set(mediaResource);
    }
    
    public static void main(String[] args) {
        launch(args);
    }

}

//Launch via this class to avoid module system headaches.
class VLCJFXVideoPlayerDemo2Launcher {public static void main(String[] args) {VLCJFXVideoPlayerDemo2.main(args);}}
