package examples.sobel;

import org.bridj.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

/**
 * Created by marcos on 03/05/17.
 */
public class SobelUtils {

    public Pointer<Float> populateImage(Pointer<Float> image_input, String filename) throws IOException {
        float[] floatValuesImage = readImageFloat(filename);
        for (int i = 0; i < floatValuesImage.length; i++)
            image_input.set(i, floatValuesImage[i]);
        return image_input;
    }

    public float[] readImageFloat(String filename) throws IOException {
        BufferedImage bi = ImageIO.read(new File(filename));
        int[] pixel;
        int i = 0;
        float[] result = new float[bi.getHeight() * bi.getWidth() * 4];

        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                pixel = bi.getRaster().getPixel(x, y, new int[4]);
                result[i] = ((float) pixel[0] / (float) Byte.MAX_VALUE);
                result[i + 1] = ((float) pixel[1] / (float) Byte.MAX_VALUE);
                result[i + 2] = ((float) pixel[2] / (float) Byte.MAX_VALUE);
                result[i + 3] = (0.0f);
                i += 4;
            }
        }
        return result;
    }

    static float clamp(float x) {
        return x < 0.0f ? 0.0f : x > 1.0f ? 1.0f : x;
    }

    public void write(Pointer<Float> image, String filename,
                             int imageWidth, int imageHeight) throws IOException {

        String fullFileName = filename+"_"+imageWidth+"x"+imageHeight+".ppm";
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        // write header
        writer.write("P3");
        writer.newLine();
        writer.write(imageWidth + " " + imageHeight);
        writer.newLine();
        writer.write("255");
        writer.newLine();

        for (int index = 0; index < (imageWidth * imageHeight * 4); index += 4) {
            writer.write((int) (clamp(image.get(index + 0)) * 255.0f + 0.5f) + " ");
            writer.write((int) (clamp(image.get(index + 1)) * 255.0f + 0.5f) + " ");
            writer.write((int) (clamp(image.get(index + 2)) * 255.0f + 0.5f) + " ");
        }
        writer.flush();
        writer.close();
    }
}
