package examples.sobel;

import com.nativelibs4java.opencl.*;
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
 * Created by marcos on 15/03/17.
 */
public class SobelJSucuriStreamMultipleQueueAsync {

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

                if(inputs[1] instanceof Object[]) {
                    CLEvent previousKernelEvent = (CLEvent)((Object[]) inputs[1])[0];
                    previousKernelEvent.waitFor();
                }

                CLContext context = (CLContext)inputs[inputs.length - 2];
                CLQueue queue = (CLQueue) inputs[inputs.length-1];
                //CLImageFormat format = new CLImageFormat(CLImageFormat.ChannelOrder.RGBA,
                //        CLImageFormat.ChannelDataType.UnsignedInt8);
                //ByteBuffer imBuffer = ByteBuffer.allocate(image.getWidth() * image.getHeight() * 24);
                //CLBuffer<Float> imBuffer = context.createBuffer(CLMem.Usage.Input, Float.class, image.getWidth()*image.getHeight()*24);



                //CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, format, image.getWidth(), image.getHeight(),
                //0, imBuffer, false);

                //CLEvent copyInImageEv = inputImage.write(queue, 0, 0, image.getWidth(), image.getHeight(), 0, imBuffer,
                //        false);
                int width = image.getWidth();
                int height = image.getHeight();
                int dataSize = height * width;
                Pointer<Integer> pixels = Pointer.pointerToInts(image.getRGB(0, 0, width, height, null, 0, width));

                CLImageFormat format = new CLImageFormat(CLImageFormat.ChannelOrder.BGRA
                        , CLImageFormat.ChannelDataType.UnsignedInt8);
                CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, format,
                        width, height, 0, pixels.getBuffer(), false);
                //CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, image, false);
                //CLEvent copyInImageEv = inputImage.write(queue, image, false, false);
                CLImageFormat imageFormat =new CLImageFormat(CLImageFormat.ChannelOrder.BGRA,
                        CLImageFormat.ChannelDataType.UNormInt8);
                //CLImageFormat.ChannelDataType channelDataType = CLImageFormat.INT_ARGB_FORMAT.getChannelDataType();
                //for (CLImageFormat.ChannelOrder channelOrder : Arrays.asList(CLImageFormat.ChannelOrder.BGRA, CLImageFormat.ChannelOrder.RGBA)) {
                  //  CLImageFormat imageFormat = new CLImageFormat(channelOrder, channelDataType);

                //CLImage2D inputImage = context.createImage2D(CLMem.Usage.Input, imageFormat, width, height);
                inputImage.write(queue, image, false);
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

                /*if(inputs[4] instanceof CLEvent) {
                    CLEvent previousKernelEvent = (CLEvent) inputs[inputs.length - 1];
                    previousKernelEvent.waitFor();
                }*/

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
                //BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                //int[] pixels = outputImage.read(queue).getInts(width * height);
                //img.setRGB(0, 0, width,height, pixels, 0, width);
                return outPtr;
            }
        };

        NodeFunction writeOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                BufferedImage outPtr = (BufferedImage)inputs[0];
                int numIter =  (Integer)inputs[1];
                try {
                    ImageIO.write(outPtr, "png", new File("outJSucuri_"+numIter+".png"));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        };

        int numWorker = new Integer(args[0]);
        int numIters = new Integer(args[1]);
        int numSimulIters = new Integer(args[2]);
        int numQueues = new Integer(args[3]);

        DFGraph dfg = new DFGraph();

        CLContext context = JavaCL.createBestContext();
        Node contextFeeder = new Feeder(context);
        dfg.add(contextFeeder);

        List<Feeder> queueFeederList = new ArrayList<Feeder>();
        for(int i = 0; i < numQueues; i++) {
            CLQueue queue = context.createDefaultQueue();
            queueFeederList.add(new Feeder(queue));
            dfg.add(queueFeederList.get(i));
        }

        Node kernelFileFeeder = new Feeder("sobel.cl");
        dfg.add(kernelFileFeeder);

        Node kernelFunctionFeeder = new Feeder("sobel_grayscale");
        dfg.add(kernelFunctionFeeder);

        Feeder feederToken = new Feeder("Token");
        dfg.add(feederToken);

        List<Feeder> imageFileFeederList = new ArrayList<Feeder>();
        List<Node> readImageNodeList = new ArrayList<Node>();
        List<Node> copyInImageNodeList = new ArrayList<Node>();
        List<Node> execKernelNodeList = new ArrayList<Node>();
        List<Node> copyOutImageNodeList = new ArrayList<Node>();
        List<Node> writeOutImageNodeList = new ArrayList<Node>();
        List<Feeder> numIterFeederList = new ArrayList<Feeder>();

        for(int i = 0; i < numIters; i++){

            try{
                BufferedReader it = new BufferedReader((new FileReader("SobelSource.txt")));
                String line;
                while ((line = it.readLine()) != null) {
                    imageFileFeederList.add(new Feeder(line));
                    dfg.add(imageFileFeederList.get(i));
                }
            }catch (Exception e){
                e.printStackTrace();
            }

            readImageNodeList.add(new Node(readImage, 1));
            dfg.add(readImageNodeList.get(i));

            copyInImageNodeList.add(new Node(copyInImage, 4));
            dfg.add(copyInImageNodeList.get(i));

            execKernelNodeList.add(new Node(execKernel, 7));
            dfg.add(execKernelNodeList.get(i));

            copyOutImageNodeList.add(new Node(copyOutImage, 2));
            dfg.add(copyOutImageNodeList.get(i));

            writeOutImageNodeList.add(new Node(writeOutImage, 2));
            dfg.add(writeOutImageNodeList.get(i));

            numIterFeederList.add(new Feeder(i));
            dfg.add(numIterFeederList.get(i));

            imageFileFeederList.get(i).add_edge(readImageNodeList.get(i), 0);

            readImageNodeList.get(i).add_edge(copyInImageNodeList.get(i), 0);
            contextFeeder.add_edge(copyInImageNodeList.get(i), 2);
            queueFeederList.get(i % numQueues).add_edge(copyInImageNodeList.get(i), 3);

            kernelFileFeeder.add_edge(execKernelNodeList.get(i), 0);
            kernelFunctionFeeder.add_edge(execKernelNodeList.get(i), 1);
            copyInImageNodeList.get(i).add_edge(execKernelNodeList.get(i), 2);
            readImageNodeList.get(i).add_edge(execKernelNodeList.get(i), 3);
            contextFeeder.add_edge(execKernelNodeList.get(i), 5);
            queueFeederList.get(i % numQueues).add_edge(execKernelNodeList.get(i), 6);

            execKernelNodeList.get(i).add_edge(copyOutImageNodeList.get(i), 0);
            queueFeederList.get(i % numQueues).add_edge(copyOutImageNodeList.get(i), 1);

            copyOutImageNodeList.get(i).add_edge(writeOutImageNodeList.get(i), 0);
            numIterFeederList.get(i).add_edge(writeOutImageNodeList.get(i), 1);

            if(i < numSimulIters){
                feederToken.add_edge(copyInImageNodeList.get(i), 1);
                feederToken.add_edge(execKernelNodeList.get(i), 4);
            }
            else{
                execKernelNodeList.get(i - numSimulIters).add_edge(copyInImageNodeList.get(i), 1);
                copyOutImageNodeList.get(i - numSimulIters).add_edge(execKernelNodeList.get(i), 4);
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
