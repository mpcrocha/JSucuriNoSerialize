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

        int image_width = 570;
        int image_height = 881;

        Pointer<Float> image_input = Pointer.allocateFloats(image_width * image_height * 4);
        SobelUtils sobelUtils = new SobelUtils();
        image_input = sobelUtils.populateImage(image_input, "chronoJudge.jpg");

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

        try {
            sobelUtils.write(outPtr, "imagemSobelFloat", image_width, image_height);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
