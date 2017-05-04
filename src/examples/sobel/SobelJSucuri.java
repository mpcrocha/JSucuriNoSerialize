package examples.sobel;

import com.nativelibs4java.opencl.*;
import examples.javaCLTest.ObjectCopyCL;
import jsucuriinoserialize.*;
import org.bridj.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Scanner;

/**
 * Created by marcos on 15/03/17.
 * numWorker inputFile imageWidth imageHeight
 * 4 chronoJudge.jpg 570 881
 */
public class SobelJSucuri {

    public static void main(String args[]){

        NodeFunction readImage = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                String fileName = (String)inputs[0];
                int imageWidth = (Integer)inputs[1];
                int imageHeight = (Integer)inputs[2];

                Pointer<Float> imageInput = Pointer.allocateFloats(imageWidth * imageHeight * 4);
                SobelUtils sobelUtils = new SobelUtils();
                try {
                    sobelUtils.populateImage(imageInput, fileName);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return imageInput;
            }
        };

        NodeFunction copyInImage = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                Pointer<Float> imageInputPointer  = (Pointer<Float>)inputs[0];
                CLContext context = (CLContext)inputs[inputs.length-2];
                CLQueue queue = (CLQueue) inputs[inputs.length-1];

                CLBuffer<Float> inputImageBuffer = context.createBuffer(CLMem.Usage.Input, imageInputPointer, false);
                CLEvent copyImageEv = inputImageBuffer.write(queue, imageInputPointer, false);
                ObjectCopyCL imageBufferEvent = new ObjectCopyCL(inputImageBuffer, copyImageEv);
                return imageBufferEvent;
            }
        };


        NodeFunction execKernel = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                System.out.println("execKernel");
                String kernelFile = (String)inputs[0];
                String kernelFunction = (String)inputs[1];
                ObjectCopyCL imageBufferEvent = (ObjectCopyCL)inputs[2];
                int imageWidth = (Integer)inputs[3];
                int imageHeight = (Integer)inputs[4];
                CLContext context = (CLContext)inputs[inputs.length-2];
                CLQueue queue = (CLQueue) inputs[inputs.length-1];

                String source = null;
                try {
                    source = new Scanner(new File(kernelFile)).useDelimiter("\\Z").next();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                CLBuffer<Float>inputImageBuffer = (CLBuffer<Float>) imageBufferEvent.getBuffer();
                CLEvent copyImageEv = imageBufferEvent.getEvent();

                CLKernel sobelKernel = context.createProgram(source).createKernel(kernelFunction);

                int[] globalWorkSizes = new int[] { imageWidth * imageHeight};
                int [] localWorkS = null;

                Pointer<Float> imageOutput = Pointer.allocateFloats(imageWidth * imageHeight * 4);
                CLBuffer<Float> outputImageBuffer = context.createBuffer(CLMem.Usage.Output, imageOutput);
                sobelKernel.setArgs(inputImageBuffer, outputImageBuffer);
                copyImageEv.waitFor();
                CLEvent kernelEv = sobelKernel.enqueueNDRange(queue, globalWorkSizes, localWorkS);

                ObjectCopyCL outputBufferEvent = new ObjectCopyCL(outputImageBuffer, kernelEv);
                return outputBufferEvent;
            }
        };


        NodeFunction copyOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                System.out.println("copyOutImage");
                ObjectCopyCL imageOutputBufferEvent = (ObjectCopyCL)inputs[0];
                int imageWidth = (Integer)inputs[1];
                int imageHeight = (Integer)inputs[2];
                CLQueue queue = (CLQueue) inputs[inputs.length -1];

                CLEvent kernelEv  = imageOutputBufferEvent.getEvent();
                CLBuffer<Float>outputImageBuffer = (CLBuffer<Float>) imageOutputBufferEvent.getBuffer();

                Pointer<Float> outputImagePointer = Pointer.allocateFloats(imageWidth * imageHeight * 4);
                kernelEv.waitFor();
                CLEvent copyOutEv = outputImageBuffer.read(queue, outputImagePointer, false);
                ObjectCopyCL imagePointerEvent = new ObjectCopyCL(outputImagePointer, copyOutEv);
                return imagePointerEvent;
            }
        };

        NodeFunction writeOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                System.out.println("Write");
                ObjectCopyCL imageOutputPointerEvent = (ObjectCopyCL)inputs[0];
                String outputFileName = (String) inputs[1];
                int imageWidth = (Integer)inputs[2];
                int imageHeight = (Integer)inputs[3];
                Pointer<Float> outputImagePointer = (Pointer<Float>) imageOutputPointerEvent.getPointer();
                SobelUtils sobelUtils = new SobelUtils();
                CLEvent copyOutEv  = imageOutputPointerEvent.getEvent();
                copyOutEv.waitFor();
                try {
                    sobelUtils.write(outputImagePointer, outputFileName,imageWidth, imageHeight);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        };

        int numWorker = new Integer(args[0]);
        String inputFile = args[1];
        int imageWidth = new Integer(args[2]);
        int imageHeight = new Integer(args[3]);

        DFGraph dfg = new DFGraph();

        Node imageFileFeeder = new Feeder(inputFile);
        dfg.add(imageFileFeeder);

        Node imageWidthFeeder = new Feeder(imageWidth);
        dfg.add(imageWidthFeeder);

        Node imageHeightFeeder = new Feeder(imageHeight);
        dfg.add(imageHeightFeeder);

        CLContext context = JavaCL.createBestContext();
        Node contextFeeder = new Feeder(context);
        dfg.add(contextFeeder);

        Node ouputImageFeeder = new Feeder("OutputJSucuriSobel");
        dfg.add(ouputImageFeeder);

        CLQueue queue = context.createDefaultQueue();
        Node queueFeeder = new Feeder(queue);
        dfg.add(queueFeeder);

        Node kernelFileFeeder = new Feeder("sobelFloat.cl");
        dfg.add(kernelFileFeeder);

        Node kernelFunctionFeeder = new Feeder("sobel_grayscale");
        dfg.add(kernelFunctionFeeder);

        Node readImageNode = new Node(readImage, 3);
        dfg.add(readImageNode);

        Node copyInImageNode = new Node(copyInImage, 3);
        dfg.add(copyInImageNode);

        Node execKernelNode = new Node(execKernel, 7);
        dfg.add(execKernelNode);

        Node copyOutImageNode = new Node(copyOutImage, 4);
        dfg.add(copyOutImageNode);

        Node writeOutImageNode = new Node(writeOutImage, 4);
        dfg.add(writeOutImageNode);

        imageFileFeeder.add_edge(readImageNode, 0);
        imageWidthFeeder.add_edge(readImageNode, 1);
        imageHeightFeeder.add_edge(readImageNode, 2);

        readImageNode.add_edge(copyInImageNode, 0);
        contextFeeder.add_edge(copyInImageNode, 1);
        queueFeeder.add_edge(copyInImageNode, 2);

        kernelFileFeeder.add_edge(execKernelNode, 0);
        kernelFunctionFeeder.add_edge(execKernelNode, 1);
        copyInImageNode.add_edge(execKernelNode, 2);
        imageWidthFeeder.add_edge(execKernelNode, 3);
        imageHeightFeeder.add_edge(execKernelNode, 4);
        contextFeeder.add_edge(execKernelNode, 5);
        queueFeeder.add_edge(execKernelNode, 6);

        execKernelNode.add_edge(copyOutImageNode, 0);
        imageWidthFeeder.add_edge(copyOutImageNode, 1);
        imageHeightFeeder.add_edge(copyOutImageNode, 2);
        queueFeeder.add_edge(copyOutImageNode, 3);

        copyOutImageNode.add_edge(writeOutImageNode, 0);
        ouputImageFeeder.add_edge(writeOutImageNode, 1);
        imageWidthFeeder.add_edge(writeOutImageNode, 2);
        imageHeightFeeder.add_edge(writeOutImageNode, 3);

        Scheduler sched = new Scheduler(dfg, numWorker, false);

        long time1 = System.currentTimeMillis();
        sched.start();
        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

    }
}
