package examples.sobel;

import com.nativelibs4java.opencl.*;
import org.bridj.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

/**
 * Created by Marcos on 12/03/2017.
 */
public class SobelFloatWrapper {

    protected void applyFilterSobel(CLContext context) throws IOException {

        CLQueue queue = context.createDefaultQueue();
        CLKernel sobel_kernel;
        String source = new Scanner(new File("sobelFloat.cl")).useDelimiter("\\Z").next();
        sobel_kernel = context.createProgram(source).createKernel("sobel_grayscale");
        //BufferedImage image = ImageIO.read(new FileInputStream("7k.jpg"));
        // tentar pinar o buffer
        int image_width = 570;
        int image_height = 881;

        Pointer<Float> image_input = Pointer.allocateFloats(image_width * image_height * 4);
        image_input = populateImage(image_input, "chronoJudge.jpg", image_width, image_height);

        CLBuffer<Float> inputImage = context.createBuffer(CLMem.Usage.Input, image_input);

        Pointer<Float> imageOutput = Pointer.allocateFloats(image_width * image_height * 4);
        CLBuffer<Float> outputImage = context.createBuffer(CLMem.Usage.Output, imageOutput);

        sobel_kernel.setArgs(inputImage, outputImage);
        int[] globalWorkSizes = new int[] { 570 * 881 };
        int [] localWorkS = null;
        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        CLEvent kernelEv = sobel_kernel.enqueueNDRange(queue, globalWorkSizes, localWorkS);

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");


        Pointer<Float> outPtr = outputImage.read(queue);
        //ImageIO.write(outPtr, "png", new File("out.png"));
        try {
            write(outPtr, "imagemSobelFloat.ppm", image_width, image_height);
            //write(image_input, "imagemSobelFloat.ppm", image_width, image_height);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    static float clamp(float x){ return x < 0.0f ? 0.0f : x > 1.0f ? 1.0f : x; }
    public static void  write(Pointer<Float> image, String filename,
                              int imageWidth, int imageHeight) throws IOException{

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        // write header
        writer.write("P3");
        writer.newLine();
        writer.write(imageWidth+" "+imageHeight);
        writer.newLine();
        writer.write("255");
        writer.newLine();

        for(int index=0; index < (imageWidth * imageHeight * 4); index += 4){
            writer.write((int)(clamp(image.get(index + 0)) * 255.0f + 0.5f)+ " ");
            writer.write((int)(clamp(image.get(index + 1)) * 255.0f + 0.5f)+ " ");
            writer.write((int)(clamp(image.get(index + 2)) * 255.0f + 0.5f)+ " ");
        }
        writer.flush();
        writer.close();
    }

    private Pointer<Float> populateImage(Pointer<Float> image_input, String filename,
                                         int image_width , int image_height) throws IOException {
        float[] floatValuesImage = readImageFloat(filename);//loadRawFileFloats(filename, image_width * image_height * 4);
        for(int i = 0; i<floatValuesImage.length; i++)
            image_input.set(i, floatValuesImage[i]);
        return image_input;
    }

    public float[] readImageFloat(String filename) throws IOException{
        BufferedImage bi = ImageIO.read(new File(filename));
        int [] pixel;
        int i = 0;
        float[] result = new float[bi.getHeight() * bi.getWidth() * 4];

        for(int y = 0; y < bi.getHeight(); y++){
            for(int x = 0; x < bi.getWidth(); x++){
                pixel = bi.getRaster().getPixel(x, y, new int[4]);
                System.out.print(pixel[0] + "-" + pixel[1] + "-" +
                        pixel[2] + "-" + bi.getWidth()* y +x);
                result[i] = ((float) pixel[0]/ (float) Byte.MAX_VALUE);
                result[i + 1] = ((float) pixel[1]/ (float) Byte.MAX_VALUE);
                result[i + 2] = ((float) pixel[2]/ (float) Byte.MAX_VALUE);
                result[i + 3] = (0.0f);
                i += 4;
            }
        }
        return result;
    }

    public static float[] loadRawFileFloats(String filename, int bytes) throws IOException
    {
        File file = new File(filename);

        if(!file.exists())
            throw new FileNotFoundException();

        //byte array[] = new byte[(int) file.length()];
        byte array[] = new byte[(int) bytes];
        RandomAccessFile ra = new RandomAccessFile(filename,"r");

        ra.read(array);
        System.out.println("file.length: " + file.length());

        float[] result = new float[array.length];

        for(int i = 0 ; i < array.length ; i++)
        {
            result[i] = (float) ((float) array[i]/ (float) Byte.MAX_VALUE);

        }
        System.out.println("CPU: "+ result[0]);

        System.out.println(result[24] + " " + result[28] +" "+ result[32]);
        System.out.println(result[36] + "#" + result[44]);
        System.out.println(result[48] + " " + result[52] +" "+ result[56]);

        return result;
    }

}
