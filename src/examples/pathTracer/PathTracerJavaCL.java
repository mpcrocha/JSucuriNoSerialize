package examples.pathTracer;

import com.nativelibs4java.opencl.*;
import org.bridj.Pointer;

import java.io.*;
import java.util.Scanner;

/**
 * Created by Jano on 27/03/2017.
 */
public class PathTracerJavaCL {

    public static void main(String[] args){
        PathTracerJavaCL pathTracer = new PathTracerJavaCL();
        try {
            pathTracer.executePathTracerCL();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void executePathTracerCL() throws FileNotFoundException {
        CLContext context = JavaCL.createBestContext();
        CLQueue queue = context.createDefaultQueue();
        CLKernel render_kernel;
        String source = new Scanner(new File("opencl_kernel.cl")).useDelimiter("\\Z").next();
        render_kernel = context.createProgram(source).createKernel("render_kernel");

        int image_width = 1280;
        int image_height = 720;

        // allocate memory on CPU to hold image
        //cpu_output = new cl_float3[image_width * image_height];
        //Float3 cpu_output = new   Float3();
        //Primitive.Float3 cpu_output = new float[image_width * image_height * 3];
        Pointer<Pointer<Float>> cpu_output = Pointer.allocateFloats(image_width * image_height, 3);
        //Pointer<Float> cpu_output = Pointer.allocateFloats(image_width * image_height * 3);
        // Create image buffer on the OpenCL device
        //cl_output = Buffer(context, CL_MEM_WRITE_ONLY, image_width * image_height * sizeof(cl_float3));
        CLBuffer<Pointer<Float>> cl_output = context.createBuffer(CLMem.Usage.Output, cpu_output);
        //CLBuffer<Float> cl_output = context.createBuffer(CLMem.Usage.Input, cpu_output);
        //sobel_kernel.setArgs(inputImage, outputImage);

        // every pixel in the image has its own thread or "work item",
        // so the total amount of work items equals the number of pixels
        int[] globalWorkSizes = new int[] { image_width * image_height};
        int [] localWorkS = new int[]{64};

        // specify OpenCL kernel arguments
        render_kernel.setArg(0, cl_output);
        render_kernel.setArg(1, image_width);
        render_kernel.setArg(2, image_height);
        render_kernel.setArg(3, 3);

        CLEvent kernelEv = render_kernel.enqueueNDRange(queue, globalWorkSizes, localWorkS);

        Pointer<Pointer<Float>> outPtr =  cl_output.read(queue);
        //Pointer<Float> outPtr =  cl_output.read(queue);
        try {
            write(outPtr, "imagem.ppm", image_width, image_height);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    static float clamp(float x){ return x < 0.0f ? 0.0f : x > 1.0f ? 1.0f : x; }

    public static void  write(Pointer<Pointer<Float>> image, String filename, int imageWidth, int imageHeight) throws IOException{
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename)));
        // write header
        writer.write("P3");
        writer.newLine();
        writer.write(imageWidth+" "+imageHeight);
        writer.newLine();
        writer.write("255");
        writer.newLine();


            for(int index=0; index < (imageWidth * imageHeight); index ++){
                int x_coord = index % imageWidth;
                int y_coord = index / imageWidth;

                //System.out.println(x_coord +" "+ y_coord+" "+ image.get(index).get(0)+" "+ image.get(index).get(1)+" "+
                //        image.get(index).get(2));
                writer.write(Math.round(Math.floor(clamp(image.get(index).get(0)) * 255.0f + 0.5f))+" ");
                writer.write(Math.round(Math.floor(clamp(image.get(index).get(1)) * 255.0f + 0.5f))+" ");
                writer.write(Math.round(Math.floor(clamp(image.get(index).get(2)) * 255.0f + 0.5f))+" ");
                //writer.write(Math.round(Math.floor(clamp(image.get(index) + 0)* 255.0f + 0.5f))+" ");
                //writer.write(Math.round(Math.floor(clamp(image.get(index) + 1)* 255.0f + 0.5f))+" ");
                //writer.write(Math.round(Math.floor(clamp(image.get(index) + 2) * 255.0f + 0.5f))+" ");

                //if(column < columndimension - 1)writer.write(" ");
                //writer.newLine();
        }
        writer.flush();
        writer.close();
    }
}
