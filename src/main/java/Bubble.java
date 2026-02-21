import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Bubble {
    private double x, y, z;
    private double speedY;
    private double size;
    private boolean active = true;

    public Bubble(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.speedY = 50 + Math.random() * 100; // Rising speed
        this.size = 2 + Math.random() * 5;
    }

    public void update(double dt) {
        y -= speedY * dt;

        // Wiggle effect
        x += Math.sin(y * 0.05) * 20 * dt;

        if (y < -50) {
            active = false;
        }
    }

    public void draw(GraphicsContext gc) {
        double focalLength = 400;
        double scale = focalLength / (focalLength + z);

        double drawSize = size * scale;
        double centerX = 1024 / 2.0;
        double centerY = 600 / 2.0;

        double drawX = centerX + (x - centerX) * scale;
        double drawY = centerY + (y - centerY) * scale;

        gc.setFill(Color.rgb(200, 230, 255, 0.4)); // Semi-transparent blue
        gc.fillOval(drawX, drawY, drawSize, drawSize);
    }

    public boolean isActive() {
        return active;
    }

    public double getZ() {
        return z;
    }
}
