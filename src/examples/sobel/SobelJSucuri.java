package examples.sobel;

import com.nativelibs4java.opencl.*;
import jsucuriinoserialize.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

/**
 * Created by marcos on 15/03/17.
 */
public class SobelJSucuri {

    public static void main(String args[]){

        NodeFunction readImage = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                String fileName = (String)inputs[0];
                BufferedImage image = null;
                try {
                    image = ImageIO.read(new FileInputStream(fileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return image;
            }
        };

        NodeFunction copyInImage = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                BufferedImage image = (BufferedImage)inputs[0];
                CLContext context = (CLContext)inputs[inputs.length - 1];
                CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, image, false);
                return inputImage;
            }
        };


        NodeFunction execKernel = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {

                String kernelFile = (String)inputs[0];
                String kernelFunction = (String)inputs[1];
                CLImage2D inputImage = (CLImage2D) inputs[2];
                BufferedImage image = (BufferedImage)inputs[3];
                CLContext context = (CLContext)inputs[inputs.length-2];
                CLQueue queue = (CLQueue) inputs[inputs.length-1];

                int imageWidth = image.getWidth();
                int imageHeight = image.getHeight();



                String source = null;
                try {
                    source = new Scanner(new File(kernelFile)).useDelimiter("\\Z").next();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                CLKernel sobel_kernel = context.createProgram(source).createKernel(kernelFunction);
                CLImage2D outputImage = context.createImage2D(CLMem.Usage.Output, image, false);
                sobel_kernel.setArgs(inputImage, outputImage);
                int[] globalWorkSizes = new int[] { imageWidth, imageHeight};
                int [] localWorkS = null;

                CLEvent kernelEv = sobel_kernel.enqueueNDRange(queue, globalWorkSizes, localWorkS);
                return new Object[]{kernelEv, outputImage};
            }
        };


        NodeFunction copyOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                CLEvent kernelEv  = (CLEvent) ((Object[])inputs[0])[0];
                CLImage2D outputImage = (CLImage2D) ((Object[])inputs[0])[1];
                CLQueue queue = (CLQueue) inputs[1];

                BufferedImage outPtr = outputImage.read(queue, kernelEv);
                return outPtr;
            }
        };

        NodeFunction writeOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                BufferedImage outPtr = (BufferedImage)inputs[0];
                try {
                    ImageIO.write(outPtr, "png", new File("outJSucuri.png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        };

        int numWorker = new Integer(args[0]);

        DFGraph dfg = new DFGraph();

        Node imageFileFeeder = new Feeder("7k.jpg");
        dfg.add(imageFileFeeder);

        CLContext context = JavaCL.createBestContext();
        Node contextFeeder = new Feeder(context);
        dfg.add(contextFeeder);

        CLQueue queue = context.createDefaultQueue();
        Node queueFeeder = new Feeder(queue);
        dfg.add(queueFeeder);

        Node kernelFileFeeder = new Feeder("sobel.cl");
        dfg.add(kernelFileFeeder);

        Node kernelFunctionFeeder = new Feeder("sobel_grayscale");
        dfg.add(kernelFunctionFeeder);

        Node readImageNode = new Node(readImage, 1);
        dfg.add(readImageNode);

        Node copyInImageNode = new Node(copyInImage, 2);
        dfg.add(copyInImageNode);

        Node execKernelNode = new Node(execKernel, 6);
        dfg.add(execKernelNode);

        Node copyOutImageNode = new Node(copyOutImage, 2);
        dfg.add(copyOutImageNode);

        Node writeOutImageNode = new Node(writeOutImage, 1);
        dfg.add(writeOutImageNode);

        imageFileFeeder.add_edge(readImageNode, 0);

        readImageNode.add_edge(copyInImageNode, 0);
        contextFeeder.add_edge(copyInImageNode, 1);

        kernelFileFeeder.add_edge(execKernelNode, 0);
        kernelFunctionFeeder.add_edge(execKernelNode, 1);
        copyInImageNode.add_edge(execKernelNode, 2);
        readImageNode.add_edge(execKernelNode, 3);
        contextFeeder.add_edge(execKernelNode, 4);
        queueFeeder.add_edge(execKernelNode, 5);

        execKernelNode.add_edge(copyOutImageNode, 0);
        queueFeeder.add_edge(copyOutImageNode, 1);

        copyOutImageNode.add_edge(writeOutImageNode, 0);

        Scheduler sched = new Scheduler(dfg, numWorker, false);

        long time1 = System.currentTimeMillis();
        sched.start();
        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

    }
}
