package examples.sobel;

import com.nativelibs4java.opencl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Scanner;

/**
 * Created by Marcos on 12/03/2017.
 */
public class SobelWrapper  {

    protected void applyFilterSobel(CLContext context) throws IOException {

        CLQueue queue = context.createDefaultQueue();
        CLKernel sobel_kernel;
        String source = new Scanner(new File("sobel.cl")).useDelimiter("\\Z").next();
        sobel_kernel = context.createProgram(source).createKernel("sobel_grayscale");
        BufferedImage image = ImageIO.read(new FileInputStream("7k.jpg"));
        // tentar pinar o buffer

        CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, image, false);
        CLImage2D outputImage = context.createImage2D(CLMem.Usage.Output, image, false);
        sobel_kernel.setArgs(inputImage, outputImage);
        int[] globalWorkSizes = new int[] { 7360, 4128 };
        int [] localWorkS = null;
        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        CLEvent kernelEv = sobel_kernel.enqueueNDRange(queue, globalWorkSizes, localWorkS);

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

        BufferedImage outPtr = outputImage.read(queue, kernelEv);
        ImageIO.write(outPtr, "png", new File("out.png"));

    }

}
