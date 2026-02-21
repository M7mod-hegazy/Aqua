import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * REALISTIC INTERACTIVE AQUARIUM
 */
public class AquariumApp extends Application {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 600;

    private double cameraAngle = 0;
    private double targetCameraAngle = 0;

    private Image backgroundFull; // Unified background

    private List<Fish> fishes = new ArrayList<>();
    private List<Food> foodList = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();

    // Input state
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private double lastMouseX = 0;

    // Cinematic Camera State
    private boolean cinematicMode = false;
    private double bezierTime = 0;
    private double camStart = 0;
    private double camEnd = 0;

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        Canvas canvas = new Canvas(WIDTH, HEIGHT);
        GraphicsContext gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        Scene scene = new Scene(root, WIDTH, HEIGHT);

        // Input Handling
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.LEFT)
                leftPressed = true;
            if (e.getCode() == KeyCode.RIGHT)
                rightPressed = true;
            if (e.getCode() == KeyCode.C) {
                cinematicMode = !cinematicMode;
                bezierTime = 0; // Reset animation
                camStart = cameraAngle;
                camEnd = cameraAngle + Math.PI; // Spin 180
                System.out.println("Cinematic Mode: " + cinematicMode);
            }
        });

        scene.setOnKeyReleased(e -> {
            if (e.getCode() == KeyCode.LEFT)
                leftPressed = false;
            if (e.getCode() == KeyCode.RIGHT)
                rightPressed = false;
        });

        // Mouse Click Handling
        scene.setOnMousePressed(e ->

        {
            if (e.isSecondaryButtonDown()) {
                lastMouseX = e.getX();
                return;
            }

            // Spawn food at the center plane of the aquarium (rz = 0)
            // We need to un-project the mouse coordinates into 3D world space

            double focalLength = 400;
            double centerX = WIDTH / 2.0;
            double centerY = HEIGHT / 2.0;
            double centerZ = 400.0;

            // 1. Calculate scale at the center plane (rz = 0, so newZ = centerZ)
            double targetZ = centerZ;
            double scale = focalLength / (focalLength + targetZ);

            // 2. Un-project screen X/Y to rotated local coordinates (rx, ry)
            // drawX = centerX + rx * scale => rx = (drawX - centerX) / scale
            double rx = (e.getX() - centerX) / scale;
            double ry = (e.getY() - centerY) / scale; // ry is just dy since we don't rotate Y

            // 3. We assume rz = 0 (spawning on the pivot plane)
            double rz = 0;

            // 4. Inverse Rotate (rx, rz) -> (dx, dz)
            // The forward rotation was:
            // rx = dx * cos - dz * sin
            // rz = dx * sin + dz * cos
            //
            // Inverse rotation (rotate by -angle):
            // dx = rx * cos(-a) - rz * sin(-a) = rx * cos + rz * sin
            // dz = rx * sin(-a) + rz * cos(-a) = -rx * sin + rz * cos

            double cos = Math.cos(cameraAngle);
            double sin = Math.sin(cameraAngle);

            double dx = rx * cos + rz * sin;
            double dz = -rx * sin + rz * cos;

            // 5. World Coordinates
            double worldX = centerX + dx;
            double worldY = centerY + ry;
            double worldZ = centerZ + dz;

            foodList.add(new Food(worldX, worldY, worldZ));
            playSound("drop.wav", -10.0f);

            // Excite nearest fish
            Fish nearest = null;
            double minD = Double.MAX_VALUE;
            for (Fish f : fishes) {
                double dist = f.distanceTo(worldX, worldY, worldZ);
                if (dist < minD) {
                    minD = dist;
                    nearest = f;
                }
            }
            if (nearest != null)
                nearest.excite();
        });

        scene.setOnMouseDragged(e -> {
            if (e.isSecondaryButtonDown()) {
                double dx = e.getX() - lastMouseX;
                targetCameraAngle -= dx * 0.005;
                lastMouseX = e.getX();
            }
        });

        loadAssets();

        playOceanSound();

        // Game Loop
        new AnimationTimer() {
            long lastTime = 0;

            @Override
            public void handle(long now) {
                if (lastTime == 0) {
                    lastTime = now;
                    return;
                }

                double elapsedSeconds = (now - lastTime) / 1_000_000_000.0;
                lastTime = now;

                update(elapsedSeconds);
                render(gc);
            }
        }.start();

        primaryStage.setTitle("Realistic Interactive Aquarium");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadAssets() {
        try {
            // Load Unified Background
            backgroundFull = loadImage("aquarium_full_background.png");
            if (backgroundFull == null)
                backgroundFull = loadImage("background.jpg");

            // Load Fish Images (1-13)
            List<Image> fishImageList = new ArrayList<>();
            for (int i = 1; i <= 13; i++) {
                Image fishImg = loadImage("fish_type" + i + ".png");
                if (fishImg != null)
                    fishImageList.add(fishImg);
            }
            Image[] fishImages = fishImageList.toArray(new Image[0]);

            // Spawn Fish
            if (fishImages.length > 0) {
                for (int i = 0; i < 50; i++) {
                    Image randomFishImg = fishImages[(int) (Math.random() * fishImages.length)];
                    if (randomFishImg == null)
                        continue;

                    // Spread wider to account for 3D perspective narrowing
                    double startX = Math.random() * (WIDTH * 3) - WIDTH;
                    double startY = Math.random() * HEIGHT;
                    double startZ = Math.random() * 1000 - 200; // More depth variation

                    double speed = (Math.random() * 0.8 + 0.4);
                    double scale = 0.1 + Math.random() * 0.15;

                    fishes.add(new Fish(randomFishImg, startX, startY, startZ, speed, scale));
                }
            }

        } catch (Exception e) {
            System.err.println("Error loading assets: " + e.getMessage());
        }
    }

    private Image loadImage(String name) {
        File file = new File(name);
        if (file.exists()) {
            return new Image(file.toURI().toString());
        }
        return null;
    }

    private void playOceanSound() {
        try {
            File soundFile = new File("ocean.wav");
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                javax.sound.sampled.FloatControl gainControl = (javax.sound.sampled.FloatControl) clip
                        .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(-15.0f);
                clip.loop(Clip.LOOP_CONTINUOUSLY);
                clip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void playSound(String filename, float volumeReduction) {
        try {
            File soundFile = new File(filename);
            if (soundFile.exists()) {
                AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioIn);
                javax.sound.sampled.FloatControl gainControl = (javax.sound.sampled.FloatControl) clip
                        .getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
                gainControl.setValue(volumeReduction);
                clip.start();
            }
        } catch (Exception e) {
            // Ignore errors
        }
    }

    // Cubic Bezier Interpolation
    private double cubicBezier(double t, double p0, double p1, double p2, double p3) {
        double u = 1 - t;
        double tt = t * t;
        double uu = u * u;
        double uuu = uu * u;
        double ttt = tt * t;
        return (uuu * p0) + (3 * uu * t * p1) + (3 * u * tt * p2) + (ttt * p3);
    }

    private void update(double dt) {
        // Camera Logic
        if (cinematicMode) {
            bezierTime += dt * 0.1; // Slower pan
            if (bezierTime > 1.0) {
                bezierTime = 0;
                camStart = camEnd;
                camEnd = camEnd + Math.PI; // Keep spinning
            }
            double p0 = camStart;
            double p1 = camStart + (camEnd - camStart) * 0.2;
            double p2 = camEnd - (camEnd - camStart) * 0.2;
            double p3 = camEnd;

            targetCameraAngle = cubicBezier(bezierTime, p0, p1, p2, p3);
            cameraAngle = targetCameraAngle;

        } else {
            // Manual Control
            if (leftPressed)
                targetCameraAngle -= 2.0 * dt;
            if (rightPressed)
                targetCameraAngle += 2.0 * dt;
            cameraAngle += (targetCameraAngle - cameraAngle) * 5 * dt;
        }

        // Update Food
        foodList.removeIf(food -> !food.isActive());
        for (Food food : foodList) {
            food.update();
        }

        // Update Particles
        if (Math.random() < 0.2) {
            particles.add(new Particle(Math.random() * WIDTH, HEIGHT + 50, Math.random() * 500, Particle.Type.BUBBLE));
        }
        if (Math.random() < 0.5) {
            particles.add(new Particle(Math.random() * WIDTH, Math.random() * HEIGHT, Math.random() * 500,
                    Particle.Type.PLANKTON));
        }
        particles.removeIf(p -> !p.update(dt));

        // Assign Food
        java.util.Map<Fish, Food> assignments = new java.util.HashMap<>();
        for (Food food : foodList) {
            Fish closest = null;
            double minD = Double.MAX_VALUE;
            for (Fish f : fishes) {
                double dist = f.distanceTo(food.getX(), food.getY(), food.getZ());
                if (dist < minD) {
                    minD = dist;
                    closest = f;
                }
            }
            if (closest != null) {
                if (assignments.containsKey(closest)) {
                    Food current = assignments.get(closest);
                    if (minD < closest.distanceTo(current.getX(), current.getY(), current.getZ())) {
                        assignments.put(closest, food);
                    }
                } else {
                    assignments.put(closest, food);
                }
            }
        }

        // Update Fish
        for (Fish fish : fishes) {
            Food target = assignments.get(fish);
            boolean ate = fish.update(dt, WIDTH, target);
            if (ate) {
                playSound("eat.wav", -10.0f);
                for (int i = 0; i < 5; i++) {
                    particles.add(new Particle(fish.getX(), fish.getY(), fish.getZ(), Particle.Type.CRUMB));
                }
            }
        }
    }

    private void render(GraphicsContext gc) {
        // Clear Screen
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH, HEIGHT);

        // 1. Draw Unified Background
        // Static background to simulate a fixed tank while contents rotate
        if (backgroundFull != null) {
            gc.drawImage(backgroundFull, 0, 0, WIDTH, HEIGHT);
        }

        // 2. Z-Sorting Render Loop
        class RenderItem {
            Object obj;
            double z;

            RenderItem(Object o, double z) {
                this.obj = o;
                this.z = z;
            }
        }

        List<RenderItem> items = new ArrayList<>();
        double centerX = 1024 / 2.0;
        double centerZ = 400.0;
        double cos = Math.cos(cameraAngle);
        double sin = Math.sin(cameraAngle);

        // Helper to compute rotated Z
        java.util.function.Function<Double[], Double> getRotZ = (coords) -> {
            double dx = coords[0] - centerX;
            double dz = coords[1] - centerZ;
            return dx * sin + dz * cos + centerZ;
        };

        for (Fish f : fishes)
            items.add(new RenderItem(f, getRotZ.apply(new Double[] { f.getX(), f.getZ() })));
        for (Food f : foodList)
            items.add(new RenderItem(f, getRotZ.apply(new Double[] { f.getX(), f.getZ() })));
        for (Particle p : particles)
            items.add(new RenderItem(p, getRotZ.apply(new Double[] { p.getX(), p.getZ() })));

        // Sort: Far (high Z) to Near (low Z)
        items.sort((a, b) -> Double.compare(b.z, a.z));

        for (RenderItem item : items) {
            if (item.obj instanceof Fish)
                ((Fish) item.obj).draw(gc, cameraAngle);
            else if (item.obj instanceof Food)
                ((Food) item.obj).draw(gc, cameraAngle);
            else if (item.obj instanceof Particle)
                ((Particle) item.obj).draw(gc, cameraAngle);
        }

        // UI Overlay
        if (cinematicMode) {
            gc.setFill(Color.WHITE);
            gc.fillText("CINEMATIC MODE", 20, 30);
        }

        drawInstructions(gc);
    }

    private void drawInstructions(GraphicsContext gc) {
        // Modern, minimal UI at bottom center
        double boxWidth = 220;
        double boxHeight = 50;
        double x = (WIDTH - boxWidth) / 2;
        double y = HEIGHT - 70;

        // Glassmorphism background
        gc.setGlobalAlpha(0.6);
        gc.setFill(Color.rgb(10, 10, 20));
        gc.fillRoundRect(x, y, boxWidth, boxHeight, 25, 25);
        gc.setGlobalAlpha(0.3);
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1);
        gc.strokeRoundRect(x, y, boxWidth, boxHeight, 25, 25);
        gc.setGlobalAlpha(1.0);

        // Icons and Text
        double iconY = y + 15;

        // Left Click (Feed)
        drawMouseIcon(gc, x + 30, iconY, true);
        gc.setFill(Color.WHITE);
        gc.setFont(javafx.scene.text.Font.font("Arial", javafx.scene.text.FontWeight.BOLD, 12));
        gc.fillText("FEED", x + 55, iconY + 15);

        // Separator
        gc.setStroke(Color.rgb(255, 255, 255, 0.3));
        gc.strokeLine(x + 110, y + 10, x + 110, y + 40);

        // Right Click (Rotate)
        drawMouseIcon(gc, x + 130, iconY, false);
        gc.fillText("ROTATE", x + 155, iconY + 15);
    }

    private void drawMouseIcon(GraphicsContext gc, double x, double y, boolean leftClick) {
        double w = 16;
        double h = 24;

        gc.setStroke(Color.WHITE);
        gc.setLineWidth(1.5);
        gc.strokeRoundRect(x, y, w, h, 8, 8);

        gc.strokeLine(x + w / 2, y, x + w / 2, y + h / 2 - 2); // Middle line

        if (leftClick) {
            gc.setFill(Color.rgb(100, 200, 255)); // Blue highlight
            gc.fillArc(x + 1, y + 1, w / 2 - 1, 10, 90, 90, javafx.scene.shape.ArcType.ROUND);
        } else {
            gc.setFill(Color.rgb(100, 200, 255));
            gc.fillArc(x + w / 2 + 0.5, y + 1, w / 2 - 1.5, 10, 0, 90, javafx.scene.shape.ArcType.ROUND);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
