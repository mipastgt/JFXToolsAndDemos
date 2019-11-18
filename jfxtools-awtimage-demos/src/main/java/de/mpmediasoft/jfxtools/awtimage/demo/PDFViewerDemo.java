package de.mpmediasoft.jfxtools.awtimage.demo;

import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import de.mpmediasoft.jfxtools.awtimage.AWTImage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** 
 * A simple demo to show how the AWTImage class can be used to build
 * a simple PDF viewer based on Apache PDFBox.
 * 
 * @author Michael Paus
 */
public class PDFViewerDemo extends Application {
    
    private final static int IMAGE_WIDTH = 700;
    private final static int IMAGE_HEIGHT = 700;
    
    private final static java.awt.Color awtBackgroundColor = java.awt.Color.WHITE;
    
    private ImageView imageView;
	private AWTImage awtImage;	
//	private String pdfFileName;	
    private PDDocument document;    
    private PDFRenderer pdfRenderer;
    private int pageIndex;
    private FileChooser fileChooser;
    
	@Override
	public void init() {
		System.out.println("java.runtime.version: " + System.getProperty("java.runtime.version", "(undefined)"));
		System.out.println("javafx.runtime.version: " + System.getProperty("javafx.runtime.version", "(undefined)"));
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
        fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files (*.pdf)", "*.pdf"));
        Button selectPDFButton = new Button("Select PDF");
        selectPDFButton.setOnAction(e -> {
            File pdfFile = fileChooser.showOpenDialog(primaryStage);
            if (pdfFile != null) {
                open(pdfFile);
            }
        });
        
        Button pageBackwardButton = new Button("<");
        pageBackwardButton.setOnAction(e -> {
            pageIndex = Math.max(pageIndex - 1, 0);
            awtImage.update();
        });
        
        Button pageForwardButton = new Button(">");
        pageForwardButton.setOnAction(e -> {
            pageIndex = Math.min(pageIndex + 1, document.getNumberOfPages() - 1);
            awtImage.update();
        });
        
		ToolBar toolbar = new ToolBar(selectPDFButton, pageBackwardButton, pageForwardButton);
		
		BorderPane root = new BorderPane();
		root.setTop(toolbar);

		imageView = new ImageView();
        imageView.setFitWidth(IMAGE_WIDTH);
        imageView.setFitHeight(IMAGE_HEIGHT);
        imageView.setPreserveRatio(true);
		root.setCenter(imageView);
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
		
        Platform.runLater(() -> {
            // Load initial PDF if provided.
            Parameters params = getParameters();
            if (params.getRaw().size() > 0) {
                String pdfFileName = params.getRaw().get(0);
                open(new File(pdfFileName));
            }
        });
	}
	
	private void open(File pdfFile) {
        try {
            if (pdfFile.canRead()) {
                if (document != null) document.close();
                document = PDDocument.load(pdfFile);
                pdfRenderer = new PDFRenderer(document);
                pdfRenderer.setSubsamplingAllowed(true);
                pageIndex = 0;
                
                if (awtImage == null) {
                    double renderScale = imageView.getScene().getWindow().getRenderScaleX();        
                    awtImage = new AWTImage((int)(IMAGE_WIDTH * renderScale), (int)(IMAGE_HEIGHT * renderScale));       
                    awtImage.setOnUpdate(g2d -> {
                        try {
                            PDPage page = document.getPage(pageIndex);
                            PDRectangle cropbBox = page.getCropBox();
                            float widthPt = cropbBox.getWidth();
                            float widthPix = awtImage.getWidth();
                            float scaleX = widthPix / widthPt;
                            
                            float heightPt = cropbBox.getHeight();
                            float heightPix = awtImage.getHeight();
                            float scaleY = heightPix / heightPt;
                            
                            float scale = Math.min(scaleX, scaleY);
                            
                            Graphics2D g2 = (Graphics2D) awtImage.getAWTImage().getGraphics();
                            // Set background for transparent pages
                            g2.setBackground(awtBackgroundColor);
                            g2.fillRect(0, 0, awtImage.getWidth(), awtImage.getHeight());
                            // Render the selected page
                            pdfRenderer.renderPageToGraphics(pageIndex, g2, scale);
                            g2.dispose();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
    
                        return null;
                    });
                    imageView.setImage(awtImage.getFXImage());
                }
                
                awtImage.update();
            } else {
                System.err.println("The first argument of the main program must be a valid path to a PDF document.");
                System.err.println("pdfFile: " + pdfFile);
                Platform.exit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}

	public static void main(String[] args) {
		launch(args);
	}

}

class PDFViewerDemoLauncher {public static void main(String[] args) {PDFViewerDemo.main(args);}}
