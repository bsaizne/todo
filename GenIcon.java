import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.File;
import javax.imageio.ImageIO;

public class GenIcon {
    public static void main(String[] args) throws Exception {
        int size = 192;
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background rounded rect
        g.setColor(new Color(0x2A, 0x62, 0xF8));
        g.fillRoundRect(8, 8, size - 16, size - 16, 48, 48);

        // White checkmark
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(16, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        Path2D p = new Path2D.Float();
        p.moveTo(50, 96);
        p.lineTo(82, 128);
        p.lineTo(142, 68);
        g.draw(p);
        g.dispose();

        String base = "C:/Users/Administrator/.gemini/antigravity/scratch/clean_todo_app/res";
        String[] dirs = {"mipmap-hdpi", "mipmap-xhdpi", "mipmap-xxhdpi"};
        for (String d : dirs) {
            File dir = new File(base, d);
            dir.mkdirs();
            ImageIO.write(img, "png", new File(dir, "ic_launcher.png"));
        }
        System.out.println("Icons generated successfully!");
    }
}
