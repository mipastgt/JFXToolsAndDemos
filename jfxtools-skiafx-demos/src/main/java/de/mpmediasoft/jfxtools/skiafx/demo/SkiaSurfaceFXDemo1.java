package de.mpmediasoft.jfxtools.skiafx.demo;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

import org.jetbrains.skija.Color;
import org.jetbrains.skija.Paint;
import org.jetbrains.skija.PaintMode;
import org.jetbrains.skija.Path;
import org.jetbrains.skija.PathEffect;
import org.jetbrains.skija.Point;
import org.jetbrains.skija.Shader;

import de.mpmediasoft.jfxtools.skiafx.SkiaSurfaceFX;
import de.mpmediasoft.jfxtools.skiafx.SkiaSurfaceFX.RenderCallback;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

public class SkiaSurfaceFXDemo1 extends Application {
    
    public static enum Variant {Discrete, Dash, Composed, Sum, Shaders}
    
    private int WIDTH = 600, HEIGHT = 400;
    
    private Path star() {
        final float R = 115.2f, C = 128.0f;
        Path path = new Path();
        path.moveTo(C + R, C);
        for (int i = 1; i < 8; ++i) {
            float a = 2.6927937f * i;
            path.lineTo(C + R * (float)cos(a), C + R * (float)sin(a));
        }
        return path;
    }
    
    private RenderCallback starDemo(Variant variant) {
        return canvas -> {
            Paint paint = new Paint();
            paint.setPathEffect(PathEffect.makeDiscrete(10.0f, 4.0f, 0));
            paint.setStrokeWidth(2.0f);
            paint.setAntiAlias(true);
            paint.setColor(0xff4285F4);
            
            if (variant == Variant.Discrete) {
                paint.setMode(PaintMode.STROKE);
                paint.setPathEffect(PathEffect.makeDiscrete(10.0f, 4.0f, 0));
            } else if (variant == Variant.Dash) {
                paint.setMode(PaintMode.STROKE);
                paint.setPathEffect(PathEffect.makeDash(intervals, 0.0f));
            } else if (variant == Variant.Composed) {
                paint.setMode(PaintMode.STROKE);
                paint.setPathEffect(PathEffect.makeDash(intervals, 0.0f).makeCompose(PathEffect.makeDiscrete(30.0f, 12.0f, 0)));
            } else if (variant == Variant.Sum) {
                paint.setMode(PaintMode.STROKE);
                paint.setPathEffect(PathEffect.makeDiscrete(10.0f, 4.0f, 0).makeSum(PathEffect.makeDiscrete(10.0f, 4.0f, 1245)));
            } else if (variant == Variant.Shaders) {
                paint.setPathEffect(PathEffect.makeDiscrete(10.0f, 4.0f, 0));
                Point p0 = new Point(0.0f, 0.0f);
                Point p1 = new Point(256.0f, 256.0f);
                int[] colors = new int[] {Color.makeRGB(66, 133, 244), Color.makeRGB(15, 157, 88)};
                paint.setShader(Shader.makeLinearGradient(p0, p1, colors));
            }

            canvas.clear(0xFFFFFFD0);
            Path path = star();
            canvas.drawPath(path, paint);
            
            return null;
        };
    };
    
    private float[] intervals = new float[] {20.0f, 10.0f, 4.0f, 10.0f};

    private RenderCallback[] demos = new RenderCallback[] {
        canvas -> { // Demo 0
            canvas.clear(0xFFD0FFD0);
            
            Paint paint = new Paint();
            paint.setColor(0xFFFF0000);
            canvas.drawCircle(100, 100, 50, paint);
            
            return null;
        },
        canvas -> { // Demo 1
            canvas.clear(0xFFFFD0FF);
            
            canvas.drawTriangles(new Point[] {
                                   new Point(320, 70),
                                   new Point(194, 287),
                                   new Point(446, 287)
                                 },
                                 new int[] { 0xFFFF0000, 0xFF00FF00, 0xFF0000FF },
                                 new Paint());
            
            Path path = new Path().moveTo(253, 216)
                          .cubicTo(283, 163.5f, 358, 163.5f, 388, 216)
                          .cubicTo(358, 268.5f, 283, 268.5f, 253, 216)
                          .closePath();
            canvas.drawPath(path, new Paint().setColor(0xFFFFFFFF));
    
            canvas.drawCircle(320, 217, 16, new Paint().setColor(0xFF000000));
            
            return null;
        },
        starDemo(Variant.Discrete), // Demo 2
        starDemo(Variant.Dash), // Demo 3
        starDemo(Variant.Composed), // Demo 4
        starDemo(Variant.Sum), // Demo 5
        starDemo(Variant.Shaders) // Demo 6
    };
    
    private SkiaSurfaceFX surface;
    
    private int demoId = 0;
    
    @Override
    public void start(Stage primaryStage) throws Exception {
        Button nextDemoButton = new Button("Next");
        nextDemoButton.setOnAction(ev -> {
            ++demoId;
            if (demoId >= demos.length) demoId = 0;
            surface.render(demos[demoId]);
        });
        nextDemoButton.setMaxWidth(Double.MAX_VALUE);
        
        surface = new SkiaSurfaceFX(WIDTH, HEIGHT);
        ImageView imageView = new ImageView(surface.getImage());
        
        BorderPane root = new BorderPane();
        root.setTop(nextDemoButton);
        root.setCenter(imageView);
        
        Scene scene = new Scene(root);
        primaryStage.setScene(scene);
        primaryStage.show();
        
        surface.render(demos[demoId]);
    }
        
    public static void main(String[] args){
        launch(args);
    }

}

class SkiaSurfaceFXDemo1Launcher {public static void main(String[] args) {SkiaSurfaceFXDemo1.main(args);}}
