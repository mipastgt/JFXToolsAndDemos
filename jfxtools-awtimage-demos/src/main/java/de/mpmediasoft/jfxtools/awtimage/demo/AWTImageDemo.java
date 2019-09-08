package de.mpmediasoft.jfxtools.awtimage.demo;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import de.mpmediasoft.jfxtools.awtimage.AWTImage;
import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ToolBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/** 
 * A simple demo to show how the AWTImage class is supposed to be used.
 * 
 * @author Michael Paus
 */
public class AWTImageDemo extends Application {

	private AWTImage awtImage = new AWTImage(800, 600);
	
	@Override
	public void init() {
		System.out.println("java.runtime.version: " + System.getProperty("java.runtime.version", "(undefined)"));
		System.out.println("javafx.runtime.version: " + System.getProperty("javafx.runtime.version", "(undefined)"));
	}

	Color c1 = Color.red;
	Color c2 = Color.green;
	Color c3 = Color.blue;
	
	Color c;

	@Override
	public void start(Stage primaryStage) throws Exception {
		Button b1 = new Button("Full (RED)");
		b1.setOnAction(e -> {
			c = c1;
			awtImage.update();
		});
		
		Button b2 = new Button("Partial (GREEN)");
		b2.setOnAction(e -> {
			c = c2;
			awtImage.update();
		});
		
		Button b3 = new Button("Empty (BLUE)");
		b3.setOnAction(e -> {
			c = c3;
			awtImage.update();
		});
		
        Button b4 = new Button("Save to 'awtimage.png'");
        b4.setOnAction(e -> {
            try {
                ImageIO.write(awtImage.getAWTImage(), "png", new File("awtimage.png"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        
        Button b5 = new Button("Save to 'awtimage.jpg'");
        b5.setOnAction(e -> {
            try {
                // This is a work-around for a java bug if images with alpha are stored as JPEGs.
                Image fxImage = awtImage.getFXImage();
                BufferedImage awtImage = new BufferedImage((int) fxImage.getWidth(), (int) fxImage.getHeight(), BufferedImage.TYPE_INT_RGB);
                ImageIO.write(SwingFXUtils.fromFXImage(fxImage, awtImage), "jpeg", new File("awtimage.jpg"));
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        });
        
		ToolBar toolbar = new ToolBar(b1, b2, b3, b4, b5);
		
		BorderPane root = new BorderPane();
		root.setTop(toolbar);

		root.setCenter(new ImageView(awtImage.getFXImage()));
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
		
		awtImage.setOnUpdate(g2d -> {
			// This is pure AWT.
			
			g2d.setBackground(Color.decode("#F0F0FF"));
			g2d.clearRect(0, 0, awtImage.getWidth(), awtImage.getHeight());
			
			Path2D p = new Path2D.Double();
			p.moveTo(100, 100);
			p.lineTo(700, 300);
			p.lineTo(200, 500);
			p.closePath();

			g2d.setColor(c);
			g2d.fill(p);
			g2d.setColor(new Color(50, 100, 150));
			g2d.setStroke(new BasicStroke(10));
			g2d.draw(p);
			
            if (c == c1) {
				System.out.println("Full update.");
				return null; // Full
			} else if (c == c2) {
				System.out.println("Partial update.");
				return new java.awt.geom.Rectangle2D.Double(0, 0, awtImage.getWidth() / 2, awtImage.getHeight()); // Partial
			} else {
				System.out.println("Empty update.");
				return new java.awt.geom.Rectangle2D.Double(); // Empty
			}
		});
	}

	public static void main(String[] args) {
		launch(args);
	}

}

class AWTImageDemoLauncher {public static void main(String[] args) {AWTImageDemo.main(args);}}
