package examples.sobel;

import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLPlatform;
import com.nativelibs4java.opencl.JavaCL;

import java.io.IOException;

/**
 * Created by Jano on 11/03/2017.
 */
public class SobelFloatCLHost {


    public static void main(String[] args){
        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
        SobelFloatWrapper SobelFloatWrapper = null;
        try {
            SobelFloatWrapper = new SobelFloatWrapper();
            System.out.println("Tracing...");
            long time1 = System.currentTimeMillis();

            SobelFloatWrapper.applyFilterSobel(context);

            long time2 = System.currentTimeMillis();
            System.out.println("Time: " + (time2 - time1) + " ms");
            System.out.println("Time: " + (time2 - time1) / 1000 + " s");


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
