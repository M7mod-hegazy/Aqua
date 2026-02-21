import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

public class Particle {
    public enum Type {
        BUBBLE, PLANKTON, CRUMB
    }

    private double x, y, z;
    private double vx, vy, vz;
    private double size;
    private double life;
    private double maxLife;
    private Type type;
    private Color color;

    public Particle(double x, double y, double z, Type type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;

        switch (type) {
            case BUBBLE:
                this.vx = (Math.random() - 0.5) * 20;
                this.vy = -50 - Math.random() * 50; // Rise up
                this.vz = (Math.random() - 0.5) * 20;
                this.size = 5 + Math.random() * 10;
                this.maxLife = 10.0;
                this.color = Color.rgb(200, 230, 255, 0.4);
                break;
            case PLANKTON:
                this.vx = (Math.random() - 0.5) * 10;
                this.vy = (Math.random() - 0.5) * 10;
                this.vz = (Math.random() - 0.5) * 10;
                this.size = 1 + Math.random() * 2;
                this.maxLife = 20.0;
                this.color = Color.rgb(200, 255, 200, 0.2); // Greenish dust
                break;
            case CRUMB:
                this.vx = (Math.random() - 0.5) * 30;
                this.vy = 20 + Math.random() * 30; // Fall down
                this.vz = (Math.random() - 0.5) * 30;
                this.size = 2 + Math.random() * 3;
                this.maxLife = 3.0;
                this.color = Color.rgb(139, 69, 19, 0.8); // Brownish
                break;
        }
        this.life = maxLife;
    }

    public boolean update(double dt) {
        life -= dt;

        // Physics
        x += vx * dt;
        y += vy * dt;
        z += vz * dt;

        // Wiggle for bubbles
        if (type == Type.BUBBLE) {
            x += Math.sin(y * 0.05 + life) * 20 * dt;
        }

        return life > 0;
    }

    public void draw(GraphicsContext gc, double angle) {
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
        double drawSize = size * scale;

        double drawX = centerX + rx * scale;
        double drawY = centerY + (y - centerY) * scale;

        // Fade out
        double opacity = (life / maxLife) * color.getOpacity();
        gc.setFill(Color.color(color.getRed(), color.getGreen(), color.getBlue(), opacity));

        gc.fillOval(drawX - drawSize / 2, drawY - drawSize / 2, drawSize, drawSize);
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }
}
