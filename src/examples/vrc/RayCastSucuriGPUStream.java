package examples.vrc;

import com.nativelibs4java.opencl.*;
import jsucuriinoserialize.*;
import org.bridj.Pointer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by marcos on 05/02/17.
 */
public class RayCastSucuriGPUStream {

    static CLContext context = null;
    static CLQueue queueCL = null;


    public static void main(String args[]){
        int nx = 4;//256;
        int ny = 4;
        int nz = 4;

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
                System.out.println("assync copy in");
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

                if(inputs[inputs.length - 1] instanceof CLEvent) {
                    CLEvent previousKernelEvent = (CLEvent)inputs[inputs.length - 1];
                    previousKernelEvent.waitFor();
                    //queueCL.finish();
                }

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
                kernel.setArgs(bufferData, samples, grid.getP0().x,
                        grid.getP1().x, grid.getP0().y, grid.getP1().y,
                        grid.getP0().z, grid.getP1().z, grid.getNx(),
                        grid.getNy(), grid.getNz(), camera.getLookat().x,
                        camera.getLookat().y, camera.getLookat().z,
                        camera.getEye().x, camera.getEye().y, camera.getEye().z,
                        camera.getWidth(), camera.getHeight(), bufferOutput);

                CLEvent copyDataEv = (CLEvent)buffersEvents[1];

                copyDataEv.waitFor();
                Camera cam = (Camera)inputs[inputs.length-2];
                int width = cam.getWidth();
                int height = cam.getHeight();


                if(inputs[inputs.length - 1] instanceof CLEvent) {
                    CLEvent previousCopyOutEvent = (CLEvent)inputs[inputs.length - 1];
                    previousCopyOutEvent.waitFor();
                    //queueCL.finish();
                }
                CLEvent kernelEv = kernel.enqueueNDRange(queueCL, new int[]{width});
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
                imprimirPointerList(colors);
                BufferedImage im = new BufferedImage(camera.getWidth(), camera.getHeight(), BufferedImage.TYPE_3BYTE_BGR);

                int num_output_image = (Integer)inputs[2];
                System.out.println("Num out image" + num_output_image);
                File outputFile = new File("output_" + num_output_image + ".png");

                int width = camera.getWidth();
                int height = camera.getHeight();
                for (int indexWidth = 0;  indexWidth < width; indexWidth++) {
                    for (int indexHeight = 0; indexHeight < height; indexHeight++) {

                        char  r   = colors.get(3* (indexWidth * height + indexHeight) + 0);
                        //r  = r>255?255:r;
                        char  g = colors.get(3* (indexWidth * height + indexHeight) + 1);
                        //g  = g>255?255:g;
                        char  b  =  colors.get(3* (indexWidth * height + indexHeight) + 2);
                        //b = b>255?255:b;
                        java.awt.Color c = new java.awt.Color((int)r, (int)g, (int)b);
                        im.setRGB(indexWidth, indexHeight, c.getRGB());

                    }
                }

                try {
                    ImageIO.write(im, "png", outputFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //ImageIcon imageIcon = new ImageIcon(im);

                //JOptionPane.showMessageDialog(null, imageIcon, "Output image", JOptionPane.PLAIN_MESSAGE);
                return 0;


            }
        };


        int numWorkers = new Integer(args[0]);
        int numRayCastNodes = new Integer(args[1]);
        String filepath = args[2];
        int imWidth = new Integer(args[3]);
        int imHeight = new Integer(args[4]);
        int samples = new Integer(args[5]).intValue();
        int numIters = new Integer(args[6]).intValue();
        int numSimultaneousIters = new Integer(args[7]).intValue();

        imWidth = 2;
        imHeight = 2;



        initializeCLVariables();

        DFGraph graph = new DFGraph();
        Scheduler sched = new Scheduler(graph, numWorkers, false);
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
        for(int i = 0; i<numIters; i++){
            Feeder filePathFeeder = new Feeder(feederPath(filepath));
            filePathFeederList.add(filePathFeeder);
            graph.add(filePathFeederList.get(i));
            Feeder dimensionsFeeder = new Feeder(feederDimension(dimensions));
            dimensionsFeederList.add(dimensionsFeeder);
            graph.add(dimensionsFeederList.get(i));
            Node readImageNode = new Node(readImage, 2);
            readImageNodeList.add(readImageNode);
            graph.add(readImageNodeList.get(i));
            Node copyInImage = new Node(assyncCopyIN, 2);
            copyInImageList.add(copyInImage);
            graph.add(copyInImageList.get(i));
            Node kernel = new Node(assyncKernel, 5);
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
            filePathFeederList.get(i).add_edge(readImageNodeList.get(i), 0);
            dimensionsFeederList.get(i).add_edge(readImageNodeList.get(i), 1);
            readImageNodeList.get(i).add_edge(copyInImageList.get(i), 0);
            copyInImageList.get(i).add_edge(kernelList.get(i), 0);
            cameraFeederList.get(i).add_edge(kernelList.get(i), 3);
            samplesFeederList.get(i).add_edge(kernelList.get(i), 1);
            gridFeederList.get(i).add_edge(kernelList.get(i), 2);
            kernelList.get(i).add_edge(copyOutList.get(i), 0);
            cameraFeederList.get(i).add_edge(copyOutList.get(i), 1);
            numIterFeederList.get(i).add_edge(copyOutList.get(i), 2);
            if(i >= numSimultaneousIters){
                kernelList.get(i - numSimultaneousIters).add_edge(copyInImageList.get(i), 1);
                copyOutList.get(i- numSimultaneousIters).add_edge(kernelList.get(i), 4);
            }
        }
        Feeder kernelcopyInImage01Token = new Feeder(0);
        graph.add(kernelcopyInImage01Token);
        kernelcopyInImage01Token.add_edge(copyInImageList.get(0), 1);
        Feeder copyOutkernel04Token = new Feeder(0);
        graph.add(copyOutkernel04Token);
        copyOutkernel04Token.add_edge(kernelList.get(0), 4);
        Feeder kernelcopyInImage11Token = new Feeder(0);
        graph.add(kernelcopyInImage11Token);
        kernelcopyInImage11Token.add_edge(copyInImageList.get(1), 1);
        Feeder copyOutkernel14Token = new Feeder(0);
        graph.add(copyOutkernel14Token);
        copyOutkernel14Token.add_edge(kernelList.get(1), 4);


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

    private static String feederPath(String filepath){
        return filepath;
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
