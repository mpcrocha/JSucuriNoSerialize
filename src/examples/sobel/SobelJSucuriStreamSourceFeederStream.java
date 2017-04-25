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
public class SobelJSucuriStreamSourceFeederStream {
    static CLContext context = null;
    static String kernelFile = null;
    static String kernelFunction = null;
    static CLQueue queue = null;

    public static void main(String args[]){

        NodeFunction readImage = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                String fileName = (String)((TaggedValue)inputs[0]).value;
                System.out.println("readImage: "+ fileName);
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
                //CLContext context = (CLContext)((TaggedValue)inputs[inputs.length - 1]).value;
                System.out.println("copyInImage");
                CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, image, false);
                return inputImage;
            }
        };

        NodeFunction execKernel = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {

                CLImage2D inputImage = (CLImage2D) inputs[0];
                System.out.println("execKernel");

                long imageWidth = inputImage.getWidth();
                long imageHeight = inputImage.getHeight();

                String source = null;
                try {
                    source = new Scanner(new File(kernelFile)).useDelimiter("\\Z").next();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                CLKernel sobel_kernel = context.createProgram(source).createKernel(kernelFunction);
                //CLImageFormat outFmt = new CLImageFormat(CLImageFormat.ChannelOrder.RGB, CLImageFormat.ChannelDataType.SignedInt8);
                BufferedImage bIm = new BufferedImage((int)imageWidth, (int)imageHeight, BufferedImage.TYPE_INT_BGR);
                CLImage2D outputImage = context.createImage2D(CLMem.Usage.Output, bIm, false);

                sobel_kernel.setArgs(inputImage, outputImage);
                int[] globalWorkSizes = new int[] { (int)imageWidth, (int)imageHeight};
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
                //CLQueue queue = (CLQueue) ((TaggedValue)inputs[1]).value;
                System.out.println("copyOutImage");
                BufferedImage outPtr = outputImage.read(queue, kernelEv);
                return outPtr;
            }
        };

        NodeFunction writeOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                BufferedImage outPtr = (BufferedImage)inputs[0];
                System.out.println("writeOutImage");
                try {
                    ImageIO.write(outPtr, "png", new File("outJSucuri"+outPtr.getHeight()+"x"+outPtr.getWidth()+".png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        };

        int numWorker = new Integer(args[0]);

        DFGraph dfg = new DFGraph();


        Source sourceNode = new Source(getBufferedReader());

        dfg.add(sourceNode);

        context = JavaCL.createBestContext();

        queue = context.createDefaultQueue();

        kernelFile = "sobel.cl";

        kernelFunction = "sobel_grayscale";

        Node readImageNode = new Node(readImage, 1);
        dfg.add(readImageNode);

        Node copyInImageNode = new Node(copyInImage, 1);
        dfg.add(copyInImageNode);

        Node execKernelNode = new Node(execKernel, 1);
        dfg.add(execKernelNode);

        Node copyOutImageNode = new Node(copyOutImage, 1);
        dfg.add(copyOutImageNode);

        Node writeOutImageNode = new Node(writeOutImage, 1);
        dfg.add(writeOutImageNode);

        sourceNode.add_edge(readImageNode, 0);

        readImageNode.add_edge(copyInImageNode, 0);

        copyInImageNode.add_edge(execKernelNode, 0);

        execKernelNode.add_edge(copyOutImageNode, 0);

        copyOutImageNode.add_edge(writeOutImageNode, 0);

        Scheduler sched = new Scheduler(dfg, numWorker, false);

        long time1 = System.currentTimeMillis();
        sched.start();
        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

    }
    private static BufferedReader getBufferedReader(){
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader("SobelSource.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bufferedReader;
    }
}
