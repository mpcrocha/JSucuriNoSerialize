package examples.sobel;

import com.nativelibs4java.opencl.*;
import examples.javaCLTest.ObjectCopyCL;
import jsucuriinoserialize.*;
import org.bridj.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

/**
 * TODO images size passed in txt file ?
 * Created by marcos on 15/03/17.
 *
 * numWorker startFrame endFram numSimultIters numQueues imageFilePrefix imageWidth imageHeight
 *
 * 4 26 4920 2 2 inputsSobelJPG/$filename 1920 1032
 * 4 26 26 1 1 inputsSobelJPG/$filename 1920 1032
 * 4 10 10 1 1 inputsSobelJPG/$filename 570 881
 */
public class SobelJsucuriStreamAnimation {

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
                    System.out.println(fileName);
                    e.printStackTrace();
                }
                return imageInput;
            }
        };

        NodeFunction copyInImage = new NodeFunction() {
            @Override
            public Object  f(Object[] inputs) {
                ObjectCopyCL outputBufferEvent = (ObjectCopyCL)inputs[1];
                if(outputBufferEvent.getEvent() != null) {
                    CLEvent previousKernelEvent = outputBufferEvent.getEvent();
                    previousKernelEvent.waitFor();
                }

                CLContext context = (CLContext)inputs[inputs.length - 2];
                CLQueue queue = (CLQueue) inputs[inputs.length-1];
                Pointer<Float> imageInputPointer  = (Pointer<Float>)inputs[0];

                CLBuffer<Float> inputImageBuffer = (CLBuffer)inputs[2];
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
                ObjectCopyCL imageOutputEvent = (ObjectCopyCL)inputs[5];
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

                long[] globalWorkSizes = new long[] { imageWidth * imageHeight};
                long [] localWorkS = new long[]{1};

                CLBuffer<Float> outputImageBuffer = (CLBuffer)inputs[6];

                sobelKernel.setArgs(inputImageBuffer, outputImageBuffer, imageWidth, imageHeight);
                try{
                    System.out.println("Waiting copy Image");
                    copyImageEv.waitFor();
                    System.out.println("Finish Waiting copy Image");
                }catch(Exception e){
                    System.out.println("Event Status: " +
                            copyImageEv.getCommandExecutionStatusValue());
                    e.printStackTrace();
                }

                if(imageOutputEvent.getEvent() != null) {
                    CLEvent previousCopyOut = imageOutputEvent.getEvent();
                    previousCopyOut.waitFor();
                }

                CLEvent kernelEv = sobelKernel.enqueueNDRange(queue, globalWorkSizes, localWorkS, null);

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
                //System.out.println("Write");
                ObjectCopyCL imageOutputPointerEvent = (ObjectCopyCL)inputs[0];
                int numIter = (Integer)inputs[1];
                String outputFileName = "Outputs/Ouput_Sobel_Stream_"+ numIter + ".ppm";
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
        int startFrame = new Integer(args[1]);
        int endFrame = new Integer(args[2]);
        int numSimulIters = new Integer(args[3]);
        int numQueues = new Integer(args[4]);
        String imageFilePrefix = args[5];
        int imageWidth = new Integer(args[6]);
        int imageHeight = new Integer(args[7]);

        DFGraph dfg = new DFGraph();

        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
        Node contextFeeder = new Feeder(context);
        dfg.add(contextFeeder);

        List<Feeder> queueFeederList = new ArrayList<Feeder>();
        List<Feeder> inputBuferFeederList = new ArrayList<Feeder>();
        List<Feeder> outputBuferFeederList = new ArrayList<Feeder>();
        for(int i = 0; i < numQueues; i++) {
            CLQueue queue = context.createDefaultQueue();
            queueFeederList.add(new Feeder(queue));
            dfg.add(queueFeederList.get(i));
            Pointer<Float> imageInput = Pointer.allocateFloats(imageWidth * imageHeight * 4);
            CLBuffer<Float> inputImageBuffer = context.createBuffer(CLMem.Usage.Input,
                    imageInput);
            inputBuferFeederList.add(new Feeder(inputImageBuffer));
            dfg.add(inputBuferFeederList.get(i));

            CLBuffer<Float> outputImageBuffer = context.createBuffer(CLMem.Usage.Output, imageInput);
            outputBuferFeederList.add(new Feeder(outputImageBuffer));
            dfg.add(outputBuferFeederList.get(i));
        }

        Node kernelFileFeeder = new Feeder("sobelFloat.cl");
        dfg.add(kernelFileFeeder);

        Node kernelFunctionFeeder = new Feeder("sobel_grayscale");
        dfg.add(kernelFunctionFeeder);

        Feeder feederToken = new Feeder(new ObjectCopyCL());
        dfg.add(feederToken);

        Feeder feederImageWidth = new Feeder(imageWidth);
        dfg.add(feederImageWidth);

        Feeder feederImageHeight = new Feeder(imageHeight);
        dfg.add(feederImageHeight);

        List<Feeder> imageFileFeederList = new ArrayList<Feeder>();
        List<Node> readImageNodeList = new ArrayList<Node>();
        List<Node> copyInImageNodeList = new ArrayList<Node>();
        List<Node> execKernelNodeList = new ArrayList<Node>();
        List<Node> copyOutImageNodeList = new ArrayList<Node>();
        List<Node> writeOutImageNodeList = new ArrayList<Node>();
        List<Feeder> numIterFeederList = new ArrayList<Feeder>();

        for(int indFrame = startFrame, i = 0; indFrame <= endFrame; indFrame++, i++){
            String addNumberBefore = indFrame < 100? "00":indFrame<1000?"0":"";
            String completeFileName = imageFilePrefix+addNumberBefore+indFrame+".jpg";

            imageFileFeederList.add(new Feeder(completeFileName));
            dfg.add(imageFileFeederList.get(i));

            readImageNodeList.add(new Node(readImage,4));
            dfg.add(readImageNodeList.get(i));

            copyInImageNodeList.add(new Node(copyInImage, 5));
            dfg.add(copyInImageNodeList.get(i));

            execKernelNodeList.add(new Node(execKernel, 9));
            dfg.add(execKernelNodeList.get(i));

            copyOutImageNodeList.add(new Node(copyOutImage, 4));
            dfg.add(copyOutImageNodeList.get(i));

            writeOutImageNodeList.add(new Node(writeOutImage, 4));
            dfg.add(writeOutImageNodeList.get(i));

            numIterFeederList.add(new Feeder(indFrame));
            dfg.add(numIterFeederList.get(i));

            imageFileFeederList.get(i).add_edge(readImageNodeList.get(i), 0);
            feederImageWidth.add_edge(readImageNodeList.get(i), 1);
            feederImageHeight.add_edge(readImageNodeList.get(i), 2);

            readImageNodeList.get(i).add_edge(copyInImageNodeList.get(i), 0);
            inputBuferFeederList.get(i % numQueues).add_edge(copyInImageNodeList.get(i), 2);
            contextFeeder.add_edge(copyInImageNodeList.get(i), 3);
            queueFeederList.get(i % numQueues).add_edge(copyInImageNodeList.get(i), 4);

            kernelFileFeeder.add_edge(execKernelNodeList.get(i), 0);
            kernelFunctionFeeder.add_edge(execKernelNodeList.get(i), 1);
            copyInImageNodeList.get(i).add_edge(execKernelNodeList.get(i), 2);
            feederImageWidth.add_edge(execKernelNodeList.get(i), 3);
            feederImageHeight.add_edge(execKernelNodeList.get(i), 4);
            outputBuferFeederList.get(i % numQueues).add_edge(execKernelNodeList.get(i), 6);
            contextFeeder.add_edge(execKernelNodeList.get(i), 7);
            queueFeederList.get(i % numQueues).add_edge(execKernelNodeList.get(i), 8);

            execKernelNodeList.get(i).add_edge(copyOutImageNodeList.get(i), 0);
            feederImageWidth.add_edge(copyOutImageNodeList.get(i), 1);
            feederImageHeight.add_edge(copyOutImageNodeList.get(i), 2);
            queueFeederList.get(i % numQueues).add_edge(copyOutImageNodeList.get(i), 3);

            copyOutImageNodeList.get(i).add_edge(writeOutImageNodeList.get(i), 0);
            numIterFeederList.get(i).add_edge(writeOutImageNodeList.get(i), 1);
            feederImageWidth.add_edge(writeOutImageNodeList.get(i), 2);
            feederImageHeight.add_edge(writeOutImageNodeList.get(i), 3);

            if(i < numSimulIters){
                feederToken.add_edge(readImageNodeList.get(i), 3);
                feederToken.add_edge(copyInImageNodeList.get(i), 1);
                feederToken.add_edge(execKernelNodeList.get(i), 5);

            }
            else{
                copyInImageNodeList.get(i - numSimulIters).add_edge(readImageNodeList.get(i), 3);
                execKernelNodeList.get(i - numSimulIters).add_edge(copyInImageNodeList.get(i), 1);
                copyOutImageNodeList.get(i - numSimulIters).add_edge(execKernelNodeList.get(i), 5);

            }
        }



        Scheduler sched = new Scheduler(dfg, numWorker, false);

        long time1 = System.currentTimeMillis();
        sched.start();
        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

    }
}
