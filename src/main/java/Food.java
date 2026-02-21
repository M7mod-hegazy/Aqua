import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Food {
    private double x, y, z;
    private double speedY = 1.0; // Falling speed
    private boolean active = true;

    public Food(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.active = true;
    }

    public Food(double x, double y) {
        this(x, y, Math.random() * 400);
    }

    public void update() {
        y += speedY;
        if (y > 600) { // Assuming HEIGHT is 600
            active = false;
        }
    }

    public void draw(GraphicsContext gc, double angle) {
        // Perspective Projection
        double focalLength = 400;
        double centerX = 1024 / 2.0;
        double centerZ = 400.0;
        double centerY = 600 / 2.0;

        double dx = x - centerX;
        double dz = z - centerZ;

        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        double rx = dx * cos - dz * sin;
        double rz = dx * sin + dz * cos;

        double newZ = rz + centerZ;

        if (newZ < -focalLength + 10)
            return;

        double scale = focalLength / (focalLength + newZ);
        double drawSize = 10 * scale;

        double drawX = centerX + rx * scale;
        double drawY = centerY + (y - centerY) * scale;

        // Better visual: Brown with a lighter center for 3D effect
        gc.setFill(Color.SADDLEBROWN);
        gc.fillOval(drawX, drawY, drawSize, drawSize);
        gc.setFill(Color.PERU);
        gc.fillOval(drawX + drawSize * 0.2, drawY + drawSize * 0.2, drawSize * 0.4, drawSize * 0.4);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public boolean isActive() {
        return active;
    }

    public void consume() {
        active = false;
    }
}
