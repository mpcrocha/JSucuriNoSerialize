package examples.vrc;

import com.nativelibs4java.opencl.*;
import examples.javaCLTest.ObjectCopyCL;
import examples.sobel.SobelUtils;
import jsucuriinoserialize.*;
import org.bridj.Pointer;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by marcos on 05/02/17.
 *
 * numWorkers imWidth imHeight samples numIters numSimultaneousIters
 *
 * 4 1280 720 1 2 2
 * 12 1280 720 1 6 6
 */
public class RayCastSucuriGPUStreamReusedBuffersMultipleQueueSeparateCopyOutWrite {
    public static void main(String args[]){
        int nx = 256;
        int ny = 256;
        int nz = 256;

        float scaleRate = 100.0f;
        final Point3d eye = new Point3d(-800.0f, -800.0f, 800.0f);
        Point3d lookat = new Point3d(0.0f, -100.0f, 0.0f);
        Point3d min = new Point3d(-1.0f*scaleRate, -1.0f*scaleRate, -1.0f*scaleRate);
        Point3d max = new Point3d(1.0f*scaleRate, 1.0f*scaleRate, 1.0f*scaleRate);

        min.scale(1.0f);
        max.scale(1.0f);

        NodeFunction readImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {

                float[] data = null;
                String filePath = (String) inputs[0];
                int[] dimensions = (int[]) inputs[1];

                int nX = dimensions[0];
                int nY = dimensions[1];
                int nZ = dimensions[2];

                try {
                    data = Util.loadRawFileFloats(filePath, nX * nY * nZ);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                if (data == null) {
                    return null;
                }
                return data;
            }
        };

        NodeFunction assyncCopyIN =  new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {

            float[] data = (float[])inputs[0];

            Pointer<Float> dataPointer = Pointer.allocateFloats(data.length);
            for(int i = 0; i < data.length; i++)
                dataPointer.set(i, data[i]);

                CLBuffer<Float> bufferData = (CLBuffer<Float>)inputs[1];
                CLQueue queueCL = (CLQueue)inputs[2];
                if(inputs[inputs.length - 1] instanceof CLEvent) {
                    CLEvent previousKernelEvent = (CLEvent)inputs[inputs.length - 1];
                    previousKernelEvent.waitFor();
                }

                CLEvent copyDataEv = bufferData.write(queueCL, dataPointer, false);

                Object[] bufferEvents = new Object[]{bufferData, copyDataEv};

                return bufferEvents;
            }

        };

        NodeFunction assyncKernel =  new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                String source = "";

                try {
                    source = new Scanner(new File("RayCastKernel.cl")).useDelimiter("\\Z").next();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                Object[] buffersEvents = (Object[])inputs[0];
                CLBuffer<Float> bufferData = (CLBuffer<Float>)buffersEvents[0];
                CLContext context = (CLContext)inputs[4];

                CLBuffer<Integer> bufferOutput = (CLBuffer<Integer>)inputs[5];

                CLKernel kernel = context.createProgram(source).createKernel("raycast");

                int samples = (Integer)inputs[1];
                Grid grid = (Grid)inputs[2];
                Camera camera = (Camera)inputs[inputs.length-2];

                CLEvent copyDataEv = (CLEvent)buffersEvents[1];

                copyDataEv.waitFor();
                //Camera cam = (Camera)inputs[inputs.length-2];
                int width = camera.getWidth();
                int height = camera.getHeight();

                int NRAN = 1024;
                int RAND_MAX = Integer.MAX_VALUE;
                int numPixels = camera.getWidth()*camera.getHeight();
                Pointer<Float> urandXPointer = Pointer.allocateFloats(NRAN);
                Pointer<Float> urandYPointer = Pointer.allocateFloats(NRAN);
                Pointer<Integer> irandPointer = Pointer.allocateInts(NRAN);
                Pointer<Integer> mDebugPointer = Pointer.allocateInts(numPixels);

                Random random = new Random();
                int i;
                for(i=0; i<NRAN; i++) urandXPointer.set(random.nextFloat() / RAND_MAX - 0.5f);
                for(i=0; i<NRAN; i++) urandYPointer.set(random.nextFloat() / RAND_MAX - 0.5f);
                for(i=0; i<NRAN; i++) irandPointer.set((int) (NRAN * (random.nextFloat() / RAND_MAX)));

                for(i = 0 ; i <numPixels ; i++)
                    mDebugPointer.set(-1);

                CLBuffer<Float> bufferUrandX = context.createBuffer(CLMem.Usage.Input, Float.class,
                        NRAN);
                CLBuffer<Float> bufferUrandY = context.createBuffer(CLMem.Usage.Input, Float.class,
                        NRAN);
                CLBuffer<Integer> bufferIrand = context.createBuffer(CLMem.Usage.Input, Integer.class,
                        NRAN);
                CLBuffer<Integer> bufferMDebug = context.createBuffer(CLMem.Usage.Input, Integer.class,
                        numPixels);

                kernel.setArgs(bufferData, samples, grid.getP0().x,
                        grid.getP1().x, grid.getP0().y, grid.getP1().y,
                        grid.getP0().z, grid.getP1().z, grid.getNx(),
                        grid.getNy(), grid.getNz(), camera.getLookat().x,
                        camera.getLookat().y, camera.getLookat().z,
                        camera.getEye().x, camera.getEye().y, camera.getEye().z,
                        camera.getWidth(), camera.getHeight(), bufferOutput, bufferUrandX,
                        bufferUrandY, bufferIrand, bufferMDebug);

                CLQueue queueCL = (CLQueue)inputs[3];
                if(inputs[inputs.length - 1] instanceof CLEvent) {
                    CLEvent previousCopyOutEvent = (CLEvent)inputs[inputs.length - 1];
                    previousCopyOutEvent.waitFor();
                    previousCopyOutEvent.release();
                }
                int[] globalWorkSizes = new int[] { width * height};
                int[] localWorkS = new int[]{64};

                CLEvent kernelEv = kernel.enqueueNDRange(queueCL, globalWorkSizes, localWorkS);
                //queueCL.finish();
                Object[] outputEvent = new Object[]{bufferOutput, kernelEv, bufferData};
                return outputEvent;
            }

        };

        NodeFunction assyncCopyOut =  new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                Object[] outputEvent = (Object[])inputs[0];
                CLBuffer<Integer> bufferOutput = (CLBuffer<Integer>) outputEvent[0];

                CLEvent kernelEv = (CLEvent)outputEvent[1];

                Camera camera = (Camera)inputs[1];
                kernelEv.waitFor();

                kernelEv.release();
                CLQueue queueCL = (CLQueue)inputs[2];
                int width = camera.getWidth();
                int height = camera.getHeight();
                Pointer<Integer> colors = Pointer.allocateInts(width * height * 4);
                CLEvent copyOutEvent = bufferOutput.read(queueCL, colors, false);
                ObjectCopyCL imagePointerEvent = new ObjectCopyCL(colors, copyOutEvent);
                return imagePointerEvent;
            }
        };

        NodeFunction writeOutImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {

                ObjectCopyCL imageOutputPointerEvent = (ObjectCopyCL)inputs[0];
                int num_output_image = (Integer)inputs[1];
                Camera camera = (Camera)inputs[2];

                int width = camera.getWidth();
                int height = camera.getHeight();

                Pointer<Integer> colors = (Pointer<Integer>) imageOutputPointerEvent.getPointer();

                CLEvent copyOutEv  = imageOutputPointerEvent.getEvent();
                copyOutEv.waitFor();

                File outputFile = new File("Outputs/output_separate_copy" + num_output_image
                        + "_"+width +"x" +height + ".ppm");
                try {
                    BufferedWriter writer = new BufferedWriter(
                            new OutputStreamWriter(
                                    new FileOutputStream(outputFile)));

                    writer.write("P3");
                    writer.newLine();
                    writer.write(width+" "+height);
                    writer.newLine();
                    writer.write("255");
                    writer.newLine();

                    for(int indexW = 0; indexW < width*height; indexW ++) {
                        writer.write(
                                (int)colors.get(3*indexW + 0) +" "+
                                        (int)colors.get(3*indexW + 1)+ " " +
                                        (int)colors.get(3*indexW + 2));
                        writer.newLine();
                    }

                    writer.flush();
                    writer.close();

                }catch (Exception e) {
                    e.printStackTrace();
                }

                return 0;
            }
        };

        int numWorkers = new Integer(args[0]);
        int imWidth = new Integer(args[1]);
        int imHeight = new Integer(args[2]);
        int samples = new Integer(args[3]).intValue();
        int numIters = new Integer(args[4]).intValue();
        int numSimultaneousIters = new Integer(args[5]).intValue();

        List<String> imagesInputList = new ArrayList<String>();
        imagesInputList.add("foot.raw");
        imagesInputList.add("skull.raw");
        imagesInputList.add("engine.raw");
        imagesInputList.add("aneurism.raw");

        List<CLQueue> inputQueuesList = new ArrayList<CLQueue>();

        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);;

        List<Feeder> bufferFeederList = new ArrayList<Feeder>();
        List<Feeder> outputBufferFeederList = new ArrayList<Feeder>();

        DFGraph graph = new DFGraph();
        Scheduler sched = new Scheduler(graph, numWorkers, false);

        for(int i = 0; i<numSimultaneousIters; i++) {

            CLBuffer<Float> inputImageBuffer = context.createBuffer(
                    CLMem.Usage.Input, Float.class, nx * ny * nz);
            bufferFeederList.add(new Feeder(inputImageBuffer));

            graph.add(bufferFeederList.get(i));


            CLBuffer<Integer> outputImageBuffer = context.createBuffer(CLMem.Usage.Output, Integer.class,
                    imWidth* imHeight *3);
            outputBufferFeederList.add(new Feeder(outputImageBuffer));

            graph.add(outputBufferFeederList.get(i));

            CLQueue queueCL = context.createDefaultQueue();;
            inputQueuesList.add(queueCL);
        }

        int[] dimensions = new int[]{nx, ny, nz};
        List<Feeder> filePathFeederList = new ArrayList<Feeder>();
        List<Feeder> dimensionsFeederList = new ArrayList<Feeder>();
        List<Node> readImageNodeList = new ArrayList<Node>();
        List<Node> copyInImageList = new ArrayList<Node>();
        List<Node> kernelList = new ArrayList<Node>();
        List<Node> copyOutList = new ArrayList<Node>();
        List<Feeder> cameraFeederList = new ArrayList<Feeder>();
        List<Feeder> gridFeederList = new ArrayList<Feeder>();
        List<Feeder> samplesFeederList = new ArrayList<Feeder>();
        List<Feeder> numIterFeederList = new ArrayList<Feeder>();

        List<Feeder> queueFeederList = new ArrayList<Feeder>();
        List<Node> writeOutImageNodeList = new ArrayList<Node>();
        Feeder contextFeeder = new Feeder(context);
        graph.add(contextFeeder);

        for(int i = 0; i < numIters; i++){
            Feeder filePathFeeder = new Feeder(imagesInputList.get(
                    i % imagesInputList.size()));
            filePathFeederList.add(filePathFeeder);
            graph.add(filePathFeederList.get(i));
            Feeder dimensionsFeeder = new Feeder(feederDimension(dimensions));
            dimensionsFeederList.add(dimensionsFeeder);
            graph.add(dimensionsFeederList.get(i));

            Feeder queueFeeder = new Feeder(inputQueuesList.get(
                    i % numSimultaneousIters));
            queueFeederList.add(queueFeeder);
            graph.add(queueFeederList.get(i));

            Node readImageNode = new Node(readImage, 3);
            readImageNodeList.add(readImageNode);
            graph.add(readImageNodeList.get(i));
            Node copyInImage = new Node(assyncCopyIN, 4);
            copyInImageList.add(copyInImage);
            graph.add(copyInImageList.get(i));
            Node kernel = new Node(assyncKernel, 8);
            kernelList.add(kernel);
            graph.add(kernelList.get(i));
            Node copyOut = new Node(assyncCopyOut, 3);
            copyOutList.add(copyOut);
            graph.add(copyOutList.get(i));
            Feeder cameraFeeder = new Feeder(cameraFeeder(imWidth, imHeight,
                    eye, lookat));
            cameraFeederList.add(cameraFeeder);
            graph.add(cameraFeederList.get(i));
            Feeder gridFeeder = new Feeder(gridFeeder(min, max, nx, ny, nz));
            gridFeederList.add(gridFeeder);
            graph.add(gridFeederList.get(i));
            Feeder numIterFeeder = new Feeder(i);
            numIterFeederList.add(numIterFeeder);
            graph.add(numIterFeederList.get(i));
            Feeder samplesFeeder = new Feeder(samplesFeeder(samples));
            samplesFeederList.add(samplesFeeder);
            graph.add(samplesFeederList.get(i));

            writeOutImageNodeList.add(new Node(writeOutImage, 3));
            graph.add(writeOutImageNodeList.get(i));

            filePathFeederList.get(i).add_edge(readImageNodeList.get(i), 0);
            dimensionsFeederList.get(i).add_edge(readImageNodeList.get(i), 1);

            readImageNodeList.get(i).add_edge(copyInImageList.get(i), 0);

            bufferFeederList.get(i % numSimultaneousIters).add_edge(copyInImageList.get(i), 1);
            queueFeederList.get(i).add_edge(copyInImageList.get(i), 2);

            copyInImageList.get(i).add_edge(kernelList.get(i), 0);
            cameraFeederList.get(i).add_edge(kernelList.get(i), 6);
            samplesFeederList.get(i).add_edge(kernelList.get(i), 1);
            gridFeederList.get(i).add_edge(kernelList.get(i), 2);
            queueFeederList.get(i).add_edge(kernelList.get(i), 3);
            contextFeeder.add_edge(kernelList.get(i), 4);
            outputBufferFeederList.get(i % numSimultaneousIters).add_edge(kernelList.get(i), 5);

            kernelList.get(i).add_edge(copyOutList.get(i), 0);
            cameraFeederList.get(i).add_edge(copyOutList.get(i), 1);
            queueFeederList.get(i).add_edge(copyOutList.get(i), 2);

            copyOutList.get(i).add_edge(writeOutImageNodeList.get(i), 0);
            numIterFeederList.get(i).add_edge(writeOutImageNodeList.get(i), 1);
            cameraFeederList.get(i).add_edge(writeOutImageNodeList.get(i), 2);

            if(i > numSimultaneousIters - 1){
                kernelList.get(i - numSimultaneousIters).add_edge(copyInImageList.get(i), 3);
                copyOutList.get(i- numSimultaneousIters).add_edge(kernelList.get(i), 7);
                copyOutList.get(i- numSimultaneousIters).add_edge(readImageNodeList.get(i), 2);
            }else{
                Feeder tokenFeeder = new Feeder(0);
                graph.add(tokenFeeder);
                tokenFeeder.add_edge(readImageNodeList.get(i), 2);
                tokenFeeder.add_edge(copyInImageList.get(i), 3);
                tokenFeeder.add_edge(kernelList.get(i), 7);
            }
        }

        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        sched.start();

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

    }

    private static int[] feederDimension(int[] dimensions){
        return dimensions;
    }

    private static Camera cameraFeeder(int imWidth, int imHeight, Point3d eye, Point3d lookat){
        return new Camera(imWidth, imHeight, eye, lookat);
    }
    private static int samplesFeeder(int samples){
        return samples;
    }

    private static Grid gridFeeder(Point3d min, Point3d max, int nx, int ny, int nz){
        return new Grid(min, max, nx, ny, nz);
    }

}
