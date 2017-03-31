package examples.vrc;

import com.nativelibs4java.opencl.*;
import jsucuriinoserialize.*;
import org.bridj.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;

/**
 * Created by marcos on 05/02/17.
 */
public class RayCastSucuriGPU {

    static CLContext context = null;
    static CLQueue queueCL = null;


    public static void main(String args[]){
        int nx = 256;//256;
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
                    // TODO Auto-generated catch block
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
                //int samples = (Integer)inputs[1];
                //Grid grid = (Grid)inputs[2];
                //Camera camera = (Camera)inputs[3];

                Pointer<Float> dataPointer = Pointer.allocateFloats(data.length);
                for(int i = 0; i < data.length; i++)
                    dataPointer.set(i, data[i]);

                CLBuffer<Float> bufferData = context.createBuffer(CLMem.Usage.Input, Float.class, dataPointer.getValidElements());
                //CLBuffer<Float> bufferStock = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);
                //CLBuffer<Float> bufferTime = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);

                CLEvent copyDataEv = bufferData.write(queueCL, dataPointer, false);
                //CLEvent copyStocksEv = bufferStock.write(queueCL, stocksPointer, false);
                //CLEvent copyTimeEv = bufferTime.write(queueCL, timesPointer, false);

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
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                Object[] buffersEvents = (Object[])inputs[0];
                CLBuffer<Float> bufferData = (CLBuffer<Float>)buffersEvents[0];

                CLBuffer<Character> bufferOutput = context.createBuffer(CLMem.Usage.Output, Character.class,
                        bufferData.getElementCount()*3);

                CLKernel kernel = context.createProgram(source).createKernel("raycast");
                //kernel.setArg(0, bufferData);
                //for(int i = 1; i<inputs.length; i++)
                    //kernel.setObjectArg(i, inputs[i]);

                //kernel.setArg(inputs.length, bufferOutput);
                //kernel.setArgs(5, bufferOptions, bufferStock, bufferTime, bufferOutput);
                int samples = (Integer)inputs[1];
                Grid grid = (Grid)inputs[2];
                Camera camera = (Camera)inputs[3];


                int NRAN = 1024;
                int RAND_MAX = Integer.MAX_VALUE;
                int numPixels = camera.getWidth()*camera.getHeight();
                Pointer<Float> urandXPointer = Pointer.allocateFloats(NRAN);
                Pointer<Float> urandYPointer = Pointer.allocateFloats(NRAN);
                Pointer<Integer> irandPointer = Pointer.allocateInts(NRAN);
                Pointer<Integer> mDebugPointer = Pointer.allocateInts(numPixels);

                Random random = new Random();
                int i,j;
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
                        NRAN);

                kernel.setArgs(bufferData, samples, grid.getP0().x,
                        grid.getP1().x, grid.getP0().y, grid.getP1().y,
                        grid.getP0().z, grid.getP1().z, grid.getNx(),
                        grid.getNy(), grid.getNz(), camera.getLookat().x,
                        camera.getLookat().y, camera.getLookat().z,
                        camera.getEye().x, camera.getEye().y, camera.getEye().z,
                        camera.getWidth(), camera.getHeight(), bufferOutput, bufferUrandX,
                        bufferUrandY, bufferIrand, bufferMDebug);

                CLEvent copyDataEv = (CLEvent)buffersEvents[1];

                copyDataEv.waitFor();
                Camera cam = (Camera)inputs[inputs.length-1];
                int width = cam.getWidth();
                int height = cam.getHeight();
                CLEvent kernelEv = kernel.enqueueNDRange(queueCL, new int[]{width});
                queueCL.finish();

                Object[] outputEvent = new Object[]{bufferOutput, kernelEv};
                return outputEvent;
            }

        };

        NodeFunction assyncCopyOut =  new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                Object[] outputEvent = (Object[])inputs[0];
                CLBuffer<Character> bufferOutput = (CLBuffer<Character>) outputEvent[0];

                CLEvent kernelEv = (CLEvent)outputEvent[1];

                Camera camera = (Camera)inputs[1];
                kernelEv.waitFor();
                Pointer<Character> colors = bufferOutput.read(queueCL);
                //imprimirPointerList(colors);
                //BufferedImage im = new BufferedImage(camera.getWidth(), camera.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

                //File outputfile = new File("output.ppm");
                // write header
                int width = camera.getWidth();
                int height = camera.getHeight();

                try {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("output.ppm")));

                    writer.write("P3");
                    writer.newLine();
                    writer.write(width+" "+height);
                    writer.newLine();
                    writer.write("255");
                    writer.newLine();

                    for(int indexW = 0; indexW < width; indexW ++) {
                        for (int indexH = 0; indexH < height; indexH++) {
                            int index = indexW*height +indexH;
                            writer.write(
                                    //Character.getNumericValue(colors.get(3*(index) + 0)) +" "+
                                    //Character.getNumericValue(colors.get(3*(index) + 0)) +" "+
                                    //Character.getNumericValue(colors.get(3 * (index) + 1))+ " " +
                                    //Character.getNumericValue(colors.get(3 * (index) + 2)));

                                    (int)colors.get(3*(index) + 0) +" "+
                                            (int)colors.get(3 * (index) + 1)+ " " +
                                            (int)colors.get(3 * (index) + 2)
                            );

                            writer.newLine();
                        }
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
        int numRayCastNodes = new Integer(args[1]);
        String filepath = args[2];
        int imWidth = new Integer(args[3]);
        int imHeight = new Integer(args[4]);
        int samples = new Integer(args[5]).intValue();

        initializeCLVariables();

        DFGraph graph = new DFGraph();
        Scheduler sched = new Scheduler(graph, numWorkers, false);

        /*
		 * BufferedReader fp = null; try { fp = new BufferedReader(new
		 * InputStreamReader(new FileInputStream("text.txt"))); } catch
		 * (FileNotFoundException e) { e.printStackTrace(); }
		 */

        // FilterTagged filter = new FilterTagged(filterPrices, 1);
        Feeder filePathFeeder = new Feeder(filepath);
        int[] dimensions = new int[]{nx, ny, nz};
        Feeder dimensionsFeeder = new Feeder(dimensions);
        Node readImageNode = new Node(readImage, 2);
        Node copyInImage = new Node(assyncCopyIN, 1);
        //Node out = new Node(writeImage, numRayCastNodes + 2);
        Node execKernelNode = new Node(assyncKernel , 4);
        Node execCopyOutNode = new Node(assyncCopyOut , 2);


        graph.add(filePathFeeder);
        graph.add(dimensionsFeeder);
        graph.add(readImageNode);
        graph.add(copyInImage);
        graph.add(execKernelNode);
        graph.add(execCopyOutNode);

        //graph.add(out);

        filePathFeeder.add_edge(readImageNode, 0);
        dimensionsFeeder.add_edge(readImageNode, 1);
        readImageNode.add_edge(copyInImage, 0);
        copyInImage.add_edge(execKernelNode, 0);

        Feeder cameraFeeder = new Feeder(new Camera(imWidth, imHeight, eye, lookat) );
        graph.add(cameraFeeder);
        //cameraFeeder.add_edge(out, numRayCastNodes);
        cameraFeeder.add_edge(execKernelNode, 3);

        Feeder samplesFeeder = new Feeder(samples);
        graph.add(samplesFeeder);

        samplesFeeder.add_edge(execKernelNode, 1);

        Feeder gridFeeder = new Feeder(new Grid(min, max, nx, ny, nz));
        graph.add(gridFeeder);
        gridFeeder.add_edge(execKernelNode, 2);

       // Feeder numRayCastNodesFeeder = new Feeder(numRayCastNodes);
        //graph.add(numRayCastNodesFeeder);

        execKernelNode.add_edge(execCopyOutNode, 0);
        cameraFeeder.add_edge(execCopyOutNode, 1);

        //numRayCastNodesFeeder.add_edge(out, numRayCastNodes + 1);

        /*
        List<Node> rayCastNodesList = new ArrayList<Node>();
        List<Feeder> feederNodesList = new ArrayList<Feeder>();

        List<Feeder> feederCameraList = new ArrayList<Feeder>();
        List<Feeder> feedeGridList = new ArrayList<Feeder>();
        */

        /*
        for (int i = 0; i < numRayCastNodes; i++) {
            feederNodesList.add(new Feeder(i));
            graph.add(feederNodesList.get(i));
            //rayCastNodesList.add(new Node(rayCastNodesFunction[i], 6));
            rayCastNodesList.add(new Node(raycast, 6));
            graph.add(rayCastNodesList.get(i));

            feederCameraList.add(new Feeder(new Camera(imWidth, imHeight, eye, lookat)));
            feedeGridList.add(new Feeder(new Grid(min, max, nx, ny, nz)));
            graph.add(feederCameraList.get(i));
            graph.add(feedeGridList.get(i));

            feederNodesList.get(i).add_edge(rayCastNodesList.get(i), 0);
            readImageNode.add_edge(rayCastNodesList.get(i), 1);
            feederCameraList.get(i).add_edge(rayCastNodesList.get(i), 2);
            samplesFeeder.add_edge(rayCastNodesList.get(i), 3);
            feedeGridList.get(i).add_edge(rayCastNodesList.get(i), 4);
            numRayCastNodesFeeder.add_edge(rayCastNodesList.get(i), 5);

            rayCastNodesList.get(i).add_edge(out, i);
        }
    */

        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        sched.start();

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

    }

    static private void initializeCLVariables(){
        context = JavaCL.createBestContext();
        queueCL = context.createDefaultQueue();
    }

    private static void imprimirPointerList(Pointer<Character> optionsPointer) {
        for (long i = 0, numEle = optionsPointer.getValidElements(); i < numEle; i++)
            System.out.println("optionsPointer.get " + i + ":" + optionsPointer.get(i));

    }

}
