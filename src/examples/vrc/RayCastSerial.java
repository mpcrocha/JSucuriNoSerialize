package examples.vrc;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class RayCastSerial {
    public static void main(String args[]) throws Exception
    {
        List<String> imagesFilesList = new ArrayList<String>();
        imagesFilesList.add("skull.raw");
        imagesFilesList.add("aneurism.raw");
        imagesFilesList.add("engine.raw");
        imagesFilesList.add("foot.raw");

        int imWidth = new Integer(args[0]);
        int imHeight = new Integer(args[1]);
        int samples = new Integer(args[2]).intValue();
        int numIters = new Integer(args[3]).intValue();

        int nx = 256;
        int ny = 256;
        int nz = 256;

        Point3d eye = new Point3d(-2000.0f, -2000.0f, 2000.0f);
        Point3d lookat = new Point3d(0.0f, -100.0f, 0.0f);
        Point3d min = new Point3d(-1.0f, -1.0f, -1.0f);
        Point3d max = new Point3d(1.0f, 1.0f, 1.0f);

        min.scale(200.0f);
        max.scale(200.0f);

        Camera cam = new Camera(imWidth, imHeight, eye, lookat);

        Grid grid = new Grid(min, max, nx, ny, nz);
        BufferedImage im = new BufferedImage(cam.getWidth(), cam.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

        for(int i = 0; i < numIters; i++){
            float[] data = readImage(imagesFilesList.get(
                    i % imagesFilesList.size()), nx, ny, nz);
            
            im = rayCast(im, cam, samples, grid, data);

            writeImage(im, imWidth, imHeight, i);
        }

    }

    private static float[] readImage(String filepath, int nx, int ny, int nz){
        float[] data = null;

        try {
            data = Util.loadRawFileFloats(filepath, nx * ny * nz);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    private static BufferedImage rayCast(BufferedImage im, Camera cam, int samples, Grid grid, float[] data){
        float rcp_samples = 1.0f / (float) samples;
        for (int i = 0; i < cam.getWidth(); i++) {
            for (int j = 0; j < cam.getHeight(); j++) {
                float r, g, b;
                r = g = b = 0.0f;

                for (int s = 0; s < samples; s++) {
                    Ray ray = cam.get_primary_ray(i, j, samples);

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

                java.awt.Color c = new java.awt.Color(r,g,b);

                im.setRGB(i, j, c.getRGB());
            }
        }
        return im;
    }

    private static void writeImage(BufferedImage im, int imWidth, int imHeight,
                                   int numIter){
        File outputFile = new File("Outputs/outputSerial_"+numIter+"_"+imWidth+"x"
                +imHeight+".png");
        try {
            ImageIO.write(im, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
