package de.mpmediasoft.jfxtools.awtimage.demo;

import java.awt.Graphics2D;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;

import de.mpmediasoft.jfxtools.awtimage.AWTImage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

/** 
 * A simple demo to show how the AWTImage class can be used to build
 * a simple PDF viewer based on Apache PDFBox.
 * 
 * @author Michael Paus
 */
public class PDFViewerDemo extends Application {
    
    private final static int IMAGE_WIDTH = 1280;
    private final static int IMAGE_HEIGHT = 720;
       
    private ImageView imageView;
	private AWTImage awtImage;	
    private PDDocument document;    
    private PDFRenderer pdfRenderer;
    private int pageIndex;
    private FileChooser fileChooser;
    private File pdfFile = null;
    private Color backgroundColor = Color.WHITE;
    
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
        selectPDFButton.setTooltip(new Tooltip("Select a PDF file."));
        selectPDFButton.setOnAction(e -> {
            pdfFile = fileChooser.showOpenDialog(primaryStage);
            if (pdfFile != null) {
                open(pdfFile);
            }
        });
        
        Button pageBackwardButton = new Button("<");
        pageBackwardButton.setTooltip(new Tooltip("Page backward."));
        pageBackwardButton.setOnAction(e -> {
            pageIndex = Math.max(pageIndex - 1, 0);
            awtImage.update();
        });
        
        Button pageForwardButton = new Button(">");
        pageForwardButton.setTooltip(new Tooltip("Page forward."));
        pageForwardButton.setOnAction(e -> {
            pageIndex = Math.min(pageIndex + 1, document.getNumberOfPages() - 1);
            awtImage.update();
        });
        
        Button saveAsPNGButton = new Button("Save");
        saveAsPNGButton.setTooltip(new Tooltip("Save rendered page as PNG image."));
        saveAsPNGButton.setOnAction(e -> {
            saveAsPNG(pdfFile);
        });
        
        ColorPicker colorPicker = new ColorPicker();
        colorPicker.setTooltip(new Tooltip("Select background color."));
        colorPicker.setValue(backgroundColor);
        colorPicker.setOnAction(a -> {
            backgroundColor = colorPicker.getValue();
            render();
        });
        
		ToolBar toolbar = new ToolBar(selectPDFButton, pageBackwardButton, pageForwardButton, saveAsPNGButton, colorPicker);
		
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
                pdfFile = new File(pdfFileName);
                open(pdfFile);
            }
        });
	}
	
	private void open(File pdfFile) {
        try {
            if (pdfFile != null && pdfFile.canRead()) {
                if (document != null) document.close();
                document = PDDocument.load(pdfFile);
                pdfRenderer = new PDFRenderer(document);
                pdfRenderer.setSubsamplingAllowed(true);
                
                render();
            } else {
                System.err.println("No valid PDF document selected.");
                System.err.println("pdfFile: " + pdfFile);
                Platform.exit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
    private void render() {
        try {
            if (pdfRenderer != null) {
                pageIndex = 0;
                
                if (awtImage == null) {
                    double renderScale = imageView.getScene().getWindow().getRenderScaleX();        
                    awtImage = new AWTImage((int)(IMAGE_WIDTH * renderScale), (int)(IMAGE_HEIGHT * renderScale));       
                    awtImage.setOnUpdate(g2d -> {
                        try {
                            PDPage page = document.getPage(pageIndex);
                            
                            PDRectangle cropBox = page.getCropBox();
                            
                            float widthPt = cropBox.getWidth();
                            float widthPix = awtImage.getWidth();
                            float scaleX = widthPix / widthPt;
                            
                            float heightPt = cropBox.getHeight();
                            float heightPix = awtImage.getHeight();
                            float scaleY = heightPix / heightPt;
                            
                            float scale = Math.min(scaleX, scaleY);
                            
                            Graphics2D g2 = (Graphics2D) awtImage.getAWTImage().getGraphics();
                            // Set background for transparent pages
                            java.awt.Color awtBackgroundColor = new java.awt.Color(
                                (float)backgroundColor.getRed(),
                                (float)backgroundColor.getGreen(),
                                (float)backgroundColor.getBlue(),
                                (float)backgroundColor.getOpacity());
                            g2.setBackground(awtBackgroundColor);
                            g2.clearRect(0, 0, awtImage.getWidth(), awtImage.getHeight());
                            
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
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
	private void saveAsPNG(File pdfFile) {
    	try {
    	    if (pdfFile != null && pdfFile.canRead()) {
    	        File parent = pdfFile.getParentFile();
                String pdfFileName = pdfFile.getName();
                String baseFileName = pdfFileName.substring(0, pdfFileName.lastIndexOf('.'));
                File pngFile = new File(parent, baseFileName + ".png");
    	        ImageIO.write(awtImage.getAWTImage(), "png", pngFile);
    	        System.out.println("PNG file written to: " + pngFile);
    	    } else {
                System.err.println("No valid PDF document selected.");
    	    }
    	} catch (Exception e) {
            e.printStackTrace();
    	}
	}
	 

	public static void main(String[] args) {
	    Locale.setDefault(Locale.US);
		launch(args);
	}

}

class PDFViewerDemoLauncher {public static void main(String[] args) {PDFViewerDemo.main(args);}}
