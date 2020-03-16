package de.mpmediasoft.jfxtools.awtimage.demo;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
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
import javafx.scene.control.TextField;
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
public class PDFViewerDemo2 extends Application {
    
    private final static int WIDTH = 1000;
    private final static int HEIGHT = 800;
        
    private final static java.awt.Color CLEAR_COLOR = new java.awt.Color(0, 0, 0, 0);
       
    private ImageView imageView;
	private AWTImage awtImage;	
    private PDDocument document;    
    private PDFRenderer pdfRenderer;
    private FileChooser fileChooser;
    private File pdfFile = null;
    private int pageIndex = 0;
    private Color backgroundColor = Color.WHITE;    
    private double rotAngleDeg = 0.0;
    private double pageScale = 1.0;
    
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
            render(pageScale, pageIndex, rotAngleDeg, backgroundColor);
        });
        
        TextField rotDegInput = new TextField();
        rotDegInput.setTooltip(new Tooltip("Select rotation angle in degrees [-180, 180]."));
        rotDegInput.setOnAction(a -> {
            try {
                rotAngleDeg = Math.min(Math.max(Double.parseDouble(rotDegInput.getText()), -180), +180);
            } catch (NumberFormatException e1) {
                rotAngleDeg = 0.0;
            }
            awtImage = null;
            render(pageScale, pageIndex, rotAngleDeg, backgroundColor);
        });
        
		ToolBar toolbar = new ToolBar(selectPDFButton, pageBackwardButton, pageForwardButton, saveAsPNGButton, colorPicker, rotDegInput);
		
		BorderPane root = new BorderPane();
		root.setTop(toolbar);

		imageView = new ImageView();
		root.setCenter(imageView);
		Scene scene = new Scene(root, WIDTH, HEIGHT);
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
                
                render(pageScale, pageIndex, rotAngleDeg, backgroundColor);
            } else {
                System.err.println("No valid PDF document selected.");
                System.err.println("pdfFile: " + pdfFile);
                Platform.exit();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
    private void render(double pageScale, int pageIndex, double rotAngleDeg, Color background) {
        try {
            if (pdfRenderer != null) {
                if (awtImage == null) {
                    PDPage page = document.getPage(pageIndex);
                    
                    PDRectangle bBox = page.getBBox();
                    
                    Rectangle2D pageRect = new Rectangle2D.Double(0.0, 0.0, bBox.getWidth() * pageScale, bBox.getHeight() * pageScale);
                    
                    final AffineTransform combTrafo;
                    
                    if (rotAngleDeg != 0.0) {
                        AffineTransform rot = AffineTransform.getRotateInstance(Math.toRadians(rotAngleDeg));                        
                        Shape rotRect = rot.createTransformedShape(pageRect);                        
                        Rectangle2D rotRectBnds = rotRect.getBounds2D();
                         
                        AffineTransform trans = AffineTransform.getTranslateInstance(-rotRectBnds.getMinX(), -rotRectBnds.getMinY());
                                                 
                        combTrafo = new AffineTransform();
                        combTrafo.concatenate(trans);
                        combTrafo.concatenate(rot);
                        
                        Shape combTrafoPageRect = combTrafo.createTransformedShape(pageRect);
                        System.out.println("combTrafoPageRect: " + combTrafoPageRect);
                        
                        awtImage = new AWTImage((int)rotRectBnds.getWidth(), (int)rotRectBnds.getHeight());
                    } else {
                        combTrafo = null;
                        awtImage = new AWTImage((int)pageRect.getWidth(), (int)pageRect.getHeight());
                    }
                    
                    awtImage.setOnUpdate(g2d -> {
                        try {                            
                            Graphics2D g2 = (Graphics2D) awtImage.getAWTImage().getGraphics();
                            g2.setBackground(CLEAR_COLOR);
                            g2.clearRect(0, 0, awtImage.getWidth(), awtImage.getHeight());

                            java.awt.Color awtBackgroundColor = new java.awt.Color(
                                (float)background.getRed(),
                                (float)background.getGreen(),
                                (float)background.getBlue(),
                                (float)background.getOpacity());
                            
                            g2.setBackground(awtBackgroundColor);
                            
                            if (combTrafo != null) {
                                g2.setTransform(combTrafo);
                            }
                            
                            // Render the selected page
                            pdfRenderer.renderPageToGraphics(pageIndex, g2, (float)pageScale);
                            
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

class PDFViewerDemo2Launcher {public static void main(String[] args) {PDFViewerDemo2.main(args);}}
