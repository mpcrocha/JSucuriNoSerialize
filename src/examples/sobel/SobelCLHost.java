package examples.sobel;

import com.nativelibs4java.opencl.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by Jano on 11/03/2017.
 */
public class SobelCLHost {


    public static void main(String[] args){
        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
        SobelWrapper sobelWrapper = null;
        try {
            sobelWrapper = new SobelWrapper();
            System.out.println("Tracing...");
            long time1 = System.currentTimeMillis();

            sobelWrapper.applyFilterSobel(context);

            long time2 = System.currentTimeMillis();
            System.out.println("Time: " + (time2 - time1) + " ms");
            System.out.println("Time: " + (time2 - time1) / 1000 + " s");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
