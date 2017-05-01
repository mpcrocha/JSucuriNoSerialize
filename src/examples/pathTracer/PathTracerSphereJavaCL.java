package examples.pathTracer;

import com.nativelibs4java.opencl.*;
import org.bridj.Pointer;

import java.io.*;
import java.util.List;
import java.util.Scanner;

/**
 * Created by marcos on 26/04/17.
 */
public class PathTracerSphereJavaCL {
    public static void main(String[] args){
        PathTracerSphereJavaCL pathTracerSphere = new PathTracerSphereJavaCL();
        try {
            pathTracerSphere.executePathTracerSphereCL();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void executePathTracerSphereCL() throws FileNotFoundException {
        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
        CLQueue queue = context.createDefaultQueue();
        CLKernel render_kernel;
        String source = new Scanner(new File("pathTracerSphere.cl")).useDelimiter("\\Z").next();
        render_kernel = context.createProgram(source).createKernel("render_kernel");

        int image_width = 1280;
        int image_height = 720;

        Pointer<Float> sphere = setSpheres3();

        CLBuffer<Float> spheres = context.createBuffer(CLMem.Usage.Input, sphere);

        Pointer<Float> cpu_output = Pointer.allocateFloats(image_width * image_height * 3);

        CLBuffer<Float> cl_output = context.createBuffer(CLMem.Usage.Output, cpu_output);
        int[] globalWorkSizes = new int[] { image_width * image_height};
        int [] localWorkS = new int[]{64};

        render_kernel.setArg(0, spheres);
        render_kernel.setArg(1, image_width);
        render_kernel.setArg(2, image_height);
        render_kernel.setArg(3, 9);
        render_kernel.setArg(4, cl_output);
        CLEvent kernelEv = render_kernel.enqueueNDRange(queue, globalWorkSizes, localWorkS);
        //CLEvent kernelEv = render_kernel.enqueueNDRange(queue, globalWorkSizes);
        //
        // kernelEv.waitFor();
        //queue.finish();
        Pointer<Float> outPtr = cl_output.read(queue);
        try {
            write(outPtr, "imagemPathSphere.ppm", image_width, image_height);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private Pointer<Float> setSpheres2() {
        int numSpheres = 13;
        Pointer<Float> sphere = Pointer.allocateFloats(10 * numSpheres);

        //left wall
        sphere.set(0, 200.0f);

        sphere.set(1, -200.6f);
        sphere.set(2, 0.0f);
        sphere.set(3, 0.0f);
        sphere.set(4, 0.0f);

        sphere.set(5, 0.75f);
        sphere.set(6, 0.25f);
        sphere.set(7, 0.25f);
        sphere.set(8, 0.0f);

        sphere.set(9, 0.0f);
        sphere.set(10, 0.0f);
        sphere.set(11, 0.0f);
        sphere.set(12, 0.0f);

        //right wall
        sphere.set(13, 200.0f);

        sphere.set(14, 200.6f);
        sphere.set(15, 0.0f);
        sphere.set(16, 0.0f);
        sphere.set(17, 0.0f);

        sphere.set(18, 0.25f);
        sphere.set(19, 0.25f);
        sphere.set(20, 0.75f);
        sphere.set(21, 0.0f);

        sphere.set(22, 0.0f);
        sphere.set(23, 0.0f);
        sphere.set(24, 0.0f);
        sphere.set(25, 0.0f);

        //floor
        sphere.set(26, 200.0f);

        sphere.set(27, 0.0f);
        sphere.set(28, -200.4f);
        sphere.set(29, 0.0f);
        sphere.set(30, 0.0f);

        sphere.set(31, 0.9f);
        sphere.set(32, 0.8f);
        sphere.set(33, 0.7f);
        sphere.set(34, 0.0f);

        sphere.set(35, 0.0f);
        sphere.set(36, 0.0f);
        sphere.set(37, 0.0f);
        sphere.set(38, 0.0f);

        //ceiling
        sphere.set(39, 200.0f);

        sphere.set(40, 0.0f);
        sphere.set(41, 200.4f);
        sphere.set(42, 0.0f);
        sphere.set(43, 0.0f);

        sphere.set(44, 0.9f);
        sphere.set(45, 0.8f);
        sphere.set(46, 0.7f);
        sphere.set(47, 0.0f);

        sphere.set(48, 0.0f);
        sphere.set(49, 0.0f);
        sphere.set(50, 0.0f);
        sphere.set(51, 0.0f);

        //back wall
        sphere.set(52, 200.0f);

        sphere.set(53, 0.0f);
        sphere.set(54, 0.0f);
        sphere.set(55, -200.4f);
        sphere.set(56, 0.0f);

        sphere.set(57, 0.9f);
        sphere.set(58, 0.8f);
        sphere.set(59, 0.7f);
        sphere.set(60, 0.0f);

        sphere.set(61, 0.0f);
        sphere.set(62, 0.0f);
        sphere.set(63, 0.0f);
        sphere.set(64, 0.0f);

        //front wall
        sphere.set(65, 200.0f);

        sphere.set(66, 0.0f);
        sphere.set(67, 0.0f);
        sphere.set(68, 202.4f);
        sphere.set(69, 0.0f);

        sphere.set(70, 0.9f);
        sphere.set(71, 0.8f);
        sphere.set(72, 0.7f);
        sphere.set(73, 0.0f);

        sphere.set(74, 0.0f);
        sphere.set(75, 0.0f);
        sphere.set(76, 0.0f);
        sphere.set(77, 0.0f);

        //left sphere
        sphere.set(78, 0.16f);

        sphere.set(79, -0.25f);
        sphere.set(80, -0.24f);
        sphere.set(81, -0.1f);
        sphere.set(82, 0.0f);

        sphere.set(83, 0.9f);
        sphere.set(84, 0.8f);
        sphere.set(85, 0.7f);
        sphere.set(86, 0.0f);

        sphere.set(87, 0.0f);
        sphere.set(88, 0.0f);
        sphere.set(89, 0.0f);
        sphere.set(90, 0.0f);

        //right sphere
        sphere.set(91, 0.16f);

        sphere.set(92, 0.25f);
        sphere.set(93, -0.24f);
        sphere.set(94, 0.1f);
        sphere.set(95, 0.0f);

        sphere.set(96, 0.9f);
        sphere.set(97, 0.8f);
        sphere.set(98, 0.7f);
        sphere.set(99, 0.0f);

        sphere.set(100, 0.0f);
        sphere.set(101, 0.0f);
        sphere.set(102, 0.0f);
        sphere.set(103, 0.0f);

        //lightsource
        sphere.set(104, 1.0f);

        sphere.set(105, 0.0f);
        sphere.set(106, 1.36f);
        sphere.set(107, 0.0f);
        sphere.set(108, 0.0f);

        sphere.set(109, 0.0f);
        sphere.set(110, 0.0f);
        sphere.set(111, 0.0f);
        sphere.set(112, 0.0f);

        sphere.set(113, 9.0f);
        sphere.set(114, 8.0f);
        sphere.set(115, 6.0f);
        sphere.set(116, 0.0f);

        return sphere;

    }

    private Pointer<Float> setSpheres3() {
        int numSpheres = 9;
        Pointer<Float> sphere = Pointer.allocateFloats(16 * numSpheres);

        //left wall
        sphere.set(0, 200.0f);
        sphere.set(1, 0.0f);
        sphere.set(2, 0.0f);
        sphere.set(3, 0.0f);

        sphere.set(4, -200.6f);
        sphere.set(5, 0.0f);
        sphere.set(6, 0.0f);
        sphere.set(7, 0.0f);

        sphere.set(8, 0.75f);
        sphere.set(9, 0.25f);
        sphere.set(10, 0.25f);
        sphere.set(11, 0.0f);

        sphere.set(12, 0.0f);
        sphere.set(13, 0.0f);
        sphere.set(14, 0.0f);
        sphere.set(15, 0.0f);

        //right wall
        sphere.set(16, 200.0f);
        sphere.set(17, 0.0f);
        sphere.set(18, 0.0f);
        sphere.set(19, 0.0f);

        sphere.set(20, 200.6f);
        sphere.set(21, 0.0f);
        sphere.set(22, 0.0f);
        sphere.set(23, 0.0f);

        sphere.set(24, 0.25f);
        sphere.set(25, 0.25f);
        sphere.set(26, 0.75f);
        sphere.set(27, 0.0f);

        sphere.set(28, 0.0f);
        sphere.set(29, 0.0f);
        sphere.set(30, 0.0f);
        sphere.set(31, 0.0f);

        //floor
        sphere.set(32, 200.0f);
        sphere.set(33, 0.0f);
        sphere.set(34, 0.0f);
        sphere.set(35, 0.0f);

        sphere.set(36, 0.0f);
        sphere.set(37, -200.4f);
        sphere.set(38, 0.0f);
        sphere.set(39, 0.0f);

        sphere.set(40, 0.9f);
        sphere.set(41, 0.8f);
        sphere.set(42, 0.7f);
        sphere.set(43, 0.0f);

        sphere.set(44, 0.0f);
        sphere.set(45, 0.0f);
        sphere.set(46, 0.0f);
        sphere.set(47, 0.0f);

        //ceiling
        sphere.set(48, 200.0f);
        sphere.set(49, 0.0f);
        sphere.set(50, 0.0f);
        sphere.set(51, 0.0f);

        sphere.set(52, 0.0f);
        sphere.set(53, 200.4f);
        sphere.set(54, 0.0f);
        sphere.set(55, 0.0f);

        sphere.set(56, 0.9f);
        sphere.set(57, 0.8f);
        sphere.set(58, 0.7f);
        sphere.set(59, 0.0f);

        sphere.set(60, 0.0f);
        sphere.set(61, 0.0f);
        sphere.set(62, 0.0f);
        sphere.set(63, 0.0f);

        //back wall
        sphere.set(64, 200.0f);
        sphere.set(65, 0.0f);
        sphere.set(66, 0.0f);
        sphere.set(67, 0.0f);

        sphere.set(68, 0.0f);
        sphere.set(69, 0.0f);
        sphere.set(70, -200.4f);
        sphere.set(71, 0.0f);

        sphere.set(72, 0.9f);
        sphere.set(73, 0.8f);
        sphere.set(74, 0.7f);
        sphere.set(75, 0.0f);

        sphere.set(76, 0.0f);
        sphere.set(77, 0.0f);
        sphere.set(78, 0.0f);
        sphere.set(79, 0.0f);

        //front wall
        sphere.set(80, 200.0f);
        sphere.set(81, 0.0f);
        sphere.set(82, 0.0f);
        sphere.set(83, 0.0f);

        sphere.set(84, 0.0f);
        sphere.set(85, 0.0f);
        sphere.set(86, 202.4f);
        sphere.set(87, 0.0f);

        sphere.set(88, 0.9f);
        sphere.set(89, 0.8f);
        sphere.set(90, 0.7f);
        sphere.set(91, 0.0f);

        sphere.set(92, 0.0f);
        sphere.set(93, 0.0f);
        sphere.set(94, 0.0f);
        sphere.set(95, 0.0f);

        //left sphere
        sphere.set(96, 0.16f);
        sphere.set(97, 0.0f);
        sphere.set(98, 0.0f);
        sphere.set(99, 0.0f);

        sphere.set(100, -0.25f);
        sphere.set(101, -0.24f);
        sphere.set(102, -0.1f);
        sphere.set(103, 0.0f);

        sphere.set(104, 0.9f);
        sphere.set(105, 0.8f);
        sphere.set(106, 0.7f);
        sphere.set(107, 0.0f);

        sphere.set(108, 0.0f);
        sphere.set(109, 0.0f);
        sphere.set(110, 0.0f);
        sphere.set(111, 0.0f);

        //right sphere
        sphere.set(112, 0.16f);
        sphere.set(113, 0.0f);
        sphere.set(114, 0.0f);
        sphere.set(115, 0.0f);

        sphere.set(116, 0.25f);
        sphere.set(117, -0.24f);
        sphere.set(118, 0.1f);
        sphere.set(119, 0.0f);

        sphere.set(120, 0.9f);
        sphere.set(121, 0.8f);
        sphere.set(122, 0.7f);
        sphere.set(123, 0.0f);

        sphere.set(124, 0.0f);
        sphere.set(125, 0.0f);
        sphere.set(126, 0.0f);
        sphere.set(127, 0.0f);

        //lightsource
        sphere.set(128, 1.0f);
        sphere.set(129, 0.0f);
        sphere.set(130, 0.0f);
        sphere.set(131, 0.0f);

        sphere.set(132, 0.0f);
        sphere.set(133, 1.36f);
        sphere.set(134, 0.0f);
        sphere.set(135, 0.0f);

        sphere.set(136, 0.0f);
        sphere.set(137, 0.0f);
        sphere.set(138, 0.0f);
        sphere.set(139, 0.0f);

        sphere.set(140, 9.0f);
        sphere.set(141, 8.0f);
        sphere.set(142, 6.0f);
        sphere.set(143, 0.0f);

        return sphere;

    }

    private Pointer<Float> setSpheres() {
        int numSpheres = 9;
        Pointer<Float> sphere = Pointer.allocateFloats(10 * numSpheres);

        //left wall
        sphere.set(0, 200.0f);

        sphere.set(1, -200.6f);
        sphere.set(2, 0.0f);
        sphere.set(3, 0.0f);

        sphere.set(4, 0.75f);
        sphere.set(5, 0.25f);
        sphere.set(6, 0.25f);

        sphere.set(7, 0.0f);
        sphere.set(8, 0.0f);
        sphere.set(9, 0.0f);

        //right wall
        sphere.set(10, 200.0f);

        sphere.set(11, 200.6f);
        sphere.set(12, 0.0f);
        sphere.set(13, 0.0f);

        sphere.set(14, 0.25f);
        sphere.set(15, 0.25f);
        sphere.set(16, 0.75f);

        sphere.set(17, 0.0f);
        sphere.set(18, 0.0f);
        sphere.set(19, 0.0f);

        //floor
        sphere.set(20, 200.0f);

        sphere.set(21, 0.0f);
        sphere.set(22, -200.4f);
        sphere.set(23, 0.0f);

        sphere.set(24, 0.9f);
        sphere.set(25, 0.8f);
        sphere.set(26, 0.7f);

        sphere.set(27, 0.0f);
        sphere.set(28, 0.0f);
        sphere.set(29, 0.0f);

        //ceiling
        sphere.set(30, 200.0f);

        sphere.set(31, 0.0f);
        sphere.set(32, 200.4f);
        sphere.set(33, 0.0f);

        sphere.set(34, 0.9f);
        sphere.set(35, 0.8f);
        sphere.set(36, 0.7f);

        sphere.set(37, 0.0f);
        sphere.set(38, 0.0f);
        sphere.set(39, 0.0f);

        //back wall
        sphere.set(40, 200.0f);

        sphere.set(41, 0.0f);
        sphere.set(42, 0.0f);
        sphere.set(43, -200.4f);

        sphere.set(44, 0.9f);
        sphere.set(45, 0.8f);
        sphere.set(46, 0.7f);

        sphere.set(47, 0.0f);
        sphere.set(48, 0.0f);
        sphere.set(49, 0.0f);

        //front wall
        sphere.set(50, 200.0f);

        sphere.set(51, 0.0f);
        sphere.set(52, 0.0f);
        sphere.set(53, 202.4f);

        sphere.set(54, 0.9f);
        sphere.set(55, 0.8f);
        sphere.set(56, 0.7f);

        sphere.set(57, 0.0f);
        sphere.set(58, 0.0f);
        sphere.set(59, 0.0f);


        //left sphere
        sphere.set(60, 0.16f);

        sphere.set(61, -0.25f);
        sphere.set(62, -0.24f);
        sphere.set(63, -0.1f);

        sphere.set(64, 0.9f);
        sphere.set(65, 0.8f);
        sphere.set(66, 0.7f);

        sphere.set(67, 0.0f);
        sphere.set(68, 0.0f);
        sphere.set(69, 0.0f);

        //right sphere
        sphere.set(70, 0.16f);

        sphere.set(71, 0.25f);
        sphere.set(72, -0.24f);
        sphere.set(73, 0.1f);

        sphere.set(74, 0.9f);
        sphere.set(75, 0.8f);
        sphere.set(76, 0.7f);

        sphere.set(77, 0.0f);
        sphere.set(78, 0.0f);
        sphere.set(79, 0.0f);

        //lightsource
        sphere.set(80, 1.0f);

        sphere.set(81, 0.0f);
        sphere.set(82, 1.36f);
        sphere.set(83, 0.0f);

        sphere.set(84, 0.0f);
        sphere.set(85, 0.0f);
        sphere.set(86, 0.0f);

        sphere.set(87, 9.0f);
        sphere.set(88, 8.0f);
        sphere.set(89, 6.0f);

        return sphere;

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

        for(int index=0; index < (imageWidth * imageHeight * 3); index += 3){
            writer.write((int)(clamp(image.get(index + 0)) * 255.0f + 0.5f)+ " ");
            writer.write((int)(clamp(image.get(index + 1)) * 255.0f + 0.5f)+ " ");
            writer.write((int)(clamp(image.get(index + 2)) * 255.0f + 0.5f)+ " ");
        }
        writer.flush();
        writer.close();
    }
}
