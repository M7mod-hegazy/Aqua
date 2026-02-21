import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;

public class ParallaxLayer {
    private Image image;
    private double scrollFactor;
    private double y;
    private double width;
    private double height;

    public ParallaxLayer(Image image, double scrollFactor, double y) {
        this.image = image;
        this.scrollFactor = scrollFactor;
        this.y = y;
        this.width = image.getWidth();
        this.height = image.getHeight();
    }

    public void draw(GraphicsContext gc, double scrollOffset, double canvasWidth, double canvasHeight) {
        if (image == null) return;

        // Calculate the effective x position based on scroll offset and factor
        double effectiveX = (scrollOffset * scrollFactor) % width;
        
        // If effectiveX is positive (scrolling left), we need to shift it back to ensure seamless looping
        if (effectiveX > 0) {
            effectiveX -= width;
        }

        // Draw the image repeated enough times to fill the canvas width
        double currentX = effectiveX;
        while (currentX < canvasWidth) {
            gc.drawImage(image, currentX, y, width, height);
            currentX += width;
        }
    }
}
