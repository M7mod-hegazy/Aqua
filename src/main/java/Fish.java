import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class Fish {
    // Physics State
    private double x, y, z;
    private double vx, vy, vz;
    private double ax, ay, az;

    // Properties
    private double width, height;
    private Image image;
    private double maxSpeed;
    private double maxForce;

    // Animation State
    private double currentScaleX = 1.0;
    private double swimTime = 0;
    private double eatAnimTimer = 0;

    // Wander State
    private double wanderTheta = 0;
    private double wanderPhi = 0;

    public Fish(Image image, double x, double y, double z, double speed, double scale) {
        this.image = image;
        this.x = x;
        this.y = y;
        this.z = z;
        this.width = image.getWidth() * scale;
        this.height = image.getHeight() * scale;

        this.maxSpeed = speed * 2.0;
        this.maxForce = 0.1; // Increased steering force

        // Initial Velocity
        double angle = Math.random() * Math.PI * 2;
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.vz = (Math.random() - 0.5) * speed;
    }

    public void excite() {
        this.eatAnimTimer = 0.5; // Visual feedback
        // Boost velocity towards current direction to simulate a "startle" or "rush"
        this.vx *= 2.0;
        this.vy *= 2.0;
        this.vz *= 2.0;
    }

    public boolean update(double dt, double canvasWidth, Food targetFood) {
        // 1. Reset Acceleration
        ax = 0;
        ay = 0;
        az = 0;

        // 2. Apply Behaviors
        boolean seeking = false;
        boolean ate = false;

        // Dynamic Max Speed/Force
        double currentMaxSpeed = maxSpeed;
        double currentMaxForce = maxForce;

        // Seek Food (Global Vision)
        if (targetFood != null && targetFood.isActive()) {
            double dist = distanceTo(targetFood.getX(), targetFood.getY(), targetFood.getZ());

            // "Speed Punch" / Excitement when close to food
            if (dist < 2000) { // Global excitement
                currentMaxSpeed *= 2.5; // Rush towards food
                currentMaxForce *= 4.0; // Much sharper turning
            }

            seek(targetFood.getX(), targetFood.getY(), targetFood.getZ(), currentMaxSpeed, currentMaxForce);
            seeking = true;

            // Easier eating: larger radius
            if (dist < 60) {
                targetFood.consume();
                ate = true;
                eatAnimTimer = 0.3;
            }
        }

        // Wander (if not seeking)
        if (!seeking) {
            wander(currentMaxForce);
        }

        // Avoid Boundaries (Always active, higher priority)
        avoidBoundaries(canvasWidth, currentMaxSpeed, currentMaxForce);

        // 3. Physics Update
        vx += ax;
        vy += ay;
        vz += az;

        // Limit Speed
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed > currentMaxSpeed) {
            double scale = currentMaxSpeed / speed;
            vx *= scale;
            vy *= scale;
            vz *= scale;
        }

        x += vx;
        y += vy;
        z += vz;

        // 4. Animation Update
        // Swim speed coupled to movement speed
        double animSpeed = 2.0 + (speed / maxSpeed) * 8.0;
        swimTime += dt * animSpeed;

        if (eatAnimTimer > 0)
            eatAnimTimer -= dt;

        // Procedural 3D Turn
        double targetScale = 0;
        if (speed > 0.1) {
            double desiredFacing = (vx >= 0) ? 1.0 : -1.0;
            double turnRate = 5.0 * dt;
            currentScaleX += (desiredFacing - currentScaleX) * turnRate;
        }

        return ate;
    }

    private void seek(double tx, double ty, double tz, double limitSpeed, double limitForce) {
        double dx = tx - (x + width / 2);
        double dy = ty - (y + height / 2);
        double dz = tz - z;

        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (dist > 0) {
            dx = (dx / dist) * limitSpeed;
            dy = (dy / dist) * limitSpeed;
            dz = (dz / dist) * limitSpeed;

            double steerX = dx - vx;
            double steerY = dy - vy;
            double steerZ = dz - vz;

            limitForce(steerX, steerY, steerZ, limitForce);
        }
    }

    private void wander(double limitForce) {
        double wanderR = 25;
        double wanderD = 80;
        double change = 0.5; // More erratic wandering

        wanderTheta += (Math.random() * 2 - 1) * change;
        wanderPhi += (Math.random() * 2 - 1) * change;

        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        double cx = 0, cy = 0, cz = 0;
        if (speed > 0) {
            cx = (vx / speed) * wanderD;
            cy = (vy / speed) * wanderD;
            cz = (vz / speed) * wanderD;
        }

        double dx = wanderR * Math.sin(wanderTheta) * Math.cos(wanderPhi);
        double dy = wanderR * Math.sin(wanderTheta) * Math.sin(wanderPhi);
        double dz = wanderR * Math.cos(wanderTheta);

        double steerX = cx + dx;
        double steerY = cy + dy;
        double steerZ = cz + dz;

        limitForce(steerX, steerY, steerZ, limitForce);
    }

    private void avoidBoundaries(double canvasWidth, double limitSpeed, double limitForce) {
        // Allow fish to go slightly off-screen before turning back
        double margin = -50;
        double turnFactor = 1.0;

        double steerX = 0, steerY = 0, steerZ = 0;

        if (x < margin)
            steerX += limitSpeed;
        if (x > canvasWidth - margin)
            steerX -= limitSpeed;

        if (y < margin)
            steerY += limitSpeed;
        if (y > 600 - margin)
            steerY -= limitSpeed;

        if (z < -100)
            steerZ += limitSpeed;
        if (z > 600)
            steerZ -= limitSpeed;

        if (steerX != 0 || steerY != 0 || steerZ != 0) {
            if (Math.abs(vz) < 0.5) {
                steerZ += (Math.random() > 0.5 ? 1 : -1) * limitSpeed;
            }
            limitForce(steerX * turnFactor, steerY * turnFactor, steerZ * turnFactor, limitForce);
        }
    }

    private void limitForce(double fx, double fy, double fz, double maxF) {
        double force = Math.sqrt(fx * fx + fy * fy + fz * fz);
        if (force > maxF) {
            double scale = maxF / force;
            fx *= scale;
            fy *= scale;
            fz *= scale;
        }
        ax += fx;
        ay += fy;
        az += fz;
    }

    public double distanceTo(double tx, double ty, double tz) {
        double dx = tx - x;
        double dy = ty - y;
        double dz = tz - z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
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

        double drawWidth = width * scale;
        double drawHeight = height * scale;

        double drawX = centerX + rx * scale;
        double drawY = centerY + (y - centerY) * scale;

        // Squash & Stretch
        double stretch = 1.0 + Math.sin(swimTime) * 0.05;
        double squash = 1.0 - Math.sin(swimTime) * 0.05;

        if (eatAnimTimer > 0) {
            double eatScale = 1.0 + Math.sin(eatAnimTimer * 20) * 0.2;
            stretch *= eatScale;
            squash *= eatScale;
        }

        // Pitch rotation (based on vertical velocity)
        double rotAngle = vy * 1.5;

        gc.save();
        gc.translate(drawX + drawWidth / 2, drawY + drawHeight / 2);
        gc.rotate(rotAngle);

        gc.scale(currentScaleX * stretch, squash);

        gc.drawImage(image, -drawWidth / 2, -drawHeight / 2, drawWidth, drawHeight);
        gc.restore();
    }

    public double getZ() {
        return z;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }
}
