package examples.vrc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class VRayCast {
    public static void main(String args[]) throws Exception
    {
        int nx = 256;
        int ny = 256;
        int nz = 256;

        float[] data;
        String filepath = "/Users/alexandrenery/uerj/rtvol/data/foot.raw";

        data = Util.loadRawFileFloats(filepath, nx * ny * nz);
        if (data == null) {
            return;
        }

        //public Camera(Point3d eye, Point3d look, int width, int height)

        Point3d eye = new Point3d(-2000.0f, -2000.0f, 2000.0f);
        Point3d lookat = new Point3d(0.0f, -100.0f, 0.0f);
        Point3d min = new Point3d(-1.0f, -1.0f, -1.0f);
        Point3d max = new Point3d(1.0f, 1.0f, 1.0f);

        min.scale(200.0f);
        max.scale(200.0f);

        Camera cam = new Camera(2000, 1000, eye, lookat);

        Grid grid = new Grid(min, max, nx, ny, nz);
        BufferedImage im = new BufferedImage(cam.getWidth(), cam.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        int samples = 1;
        float rcp_samples = 1.0f / (float) samples;

        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        for (int i = 0; i < cam.getWidth(); i++) {
            for (int j = 0; j < cam.getHeight(); j++) {
                float r, g, b;
                r = g = b = 0.0f;

                for (int s = 0; s < samples; s++) {
                    Ray ray = cam.get_primary_ray(i, j, samples);
                    //Ray ray = get_primary_ray(cam, i, j, samples);

                    //System.out.println("ray: " + ray);

                    Color c = grid.intersectGrid(ray, data, 1.0f);

                    r += c.r;
                    g += c.g;
                    b += c.b;

                }

                r = r * rcp_samples;
                g = g * rcp_samples;
                b = b * rcp_samples;

                float maxColor = Math.max(Math.max(r, g),b);
                if(maxColor > 1.0f)
                {
                    r = r / maxColor;
                    g = g / maxColor;
                    b = b / maxColor;
                }

                //System.out.println("rgb = " + r + "," + g + "," + b);

                java.awt.Color c = new java.awt.Color(r,g,b);

                im.setRGB(i, j, c.getRGB());
            }
        }

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");

        File outputfile = new File("output_low.png");
        ImageIO.write(im, "png", outputfile);

        ImageIcon image = new ImageIcon(im);

        JOptionPane.showMessageDialog(null, image, "Output image", JOptionPane.PLAIN_MESSAGE);

    }
}
