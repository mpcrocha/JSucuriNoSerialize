package examples.pathTracer;

import com.nativelibs4java.opencl.*;
import examples.javaCLTest.ObjectCopyCL;
import examples.sobel.SobelUtils;
import jsucuriinoserialize.*;
import org.bridj.Pointer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by marcos on 06/05/17.
 * numWorkers numIters numSimultaneousIters maxNumSpheres outputImageWidth outputImageHeight
 * maxScenesCreationSimultaneous
 * 4 2 2 9 1280 720 4
 */
public class PathTracerSphereJSucuri {
    public static void main(String args[]){


        NodeFunction configurationScene = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                PathTracerUtils pathTracerUtils = new PathTracerUtils();
                Pointer<Float> sphereScene = pathTracerUtils.setSpheres();

                return sphereScene;

            }
        };

        NodeFunction copySceneToGpu = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                System.out.println("cin");
                Pointer<Float> sphereScenesPointer  = (Pointer<Float>)in_operands[0];
                CLBuffer<Float> spheresSceneBuffer = (CLBuffer)in_operands[1];
                ObjectCopyCL outputBufferEvent = (ObjectCopyCL)in_operands[2];
                if(outputBufferEvent.getEvent() != null) {
                    CLEvent previousKernelEvent = outputBufferEvent.getEvent();
                    previousKernelEvent.waitFor();
                }

                CLContext context = (CLContext)in_operands[in_operands.length - 2];
                CLQueue queue = (CLQueue) in_operands[in_operands.length-1];

                CLEvent copyImageEv = spheresSceneBuffer.write(queue, sphereScenesPointer, false);
                ObjectCopyCL imageBufferEvent = new ObjectCopyCL(spheresSceneBuffer, copyImageEv);
                return imageBufferEvent;
            }
        };

        NodeFunction executeKernel = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                System.out.println("kernel");

                ObjectCopyCL sphereSceneBufferEvent = (ObjectCopyCL)in_operands[0];
                CLBuffer<Float> sphereSceneBuffer = (CLBuffer<Float>)sphereSceneBufferEvent.getBuffer();
                CLEvent copyInEvent = (CLEvent)sphereSceneBufferEvent.getEvent();

                CLBuffer<Float> outputBuffer = (CLBuffer<Float>)in_operands[1];
                CLKernel kernel = (CLKernel) in_operands[2];

                int imageWidth = (Integer) in_operands[3];
                int imageHeight = (Integer) in_operands[5];

                CLQueue queue = (CLQueue)in_operands[in_operands.length-1];

                long[] globalWorkSizes = new long[] { imageWidth * imageHeight};
                long[] localWorkS = new long[]{64};

                kernel.setArg(0, sphereSceneBuffer);
                kernel.setArg(1, imageWidth);
                kernel.setArg(2, imageHeight);
                kernel.setArg(3, 9);
                kernel.setArg(4, outputBuffer);

                ObjectCopyCL imageOutputEvent = (ObjectCopyCL)in_operands[4];

                if(imageOutputEvent.getEvent() != null) {
                    CLEvent previousCopyOut = imageOutputEvent.getEvent();
                    previousCopyOut.waitFor();
                }

                copyInEvent.waitFor();

                CLEvent kernelEv = kernel.enqueueNDRange(queue, globalWorkSizes, localWorkS, null);
                ObjectCopyCL imageBufferEvent = new ObjectCopyCL(outputBuffer, kernelEv);
                return imageBufferEvent;

            }
        };

        NodeFunction copyOutSceneFromGpu = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                System.out.println("cp out");
                ObjectCopyCL imageOutputBufferEvent = (ObjectCopyCL)in_operands[0];
                CLQueue queue = (CLQueue) in_operands[in_operands.length -1];

                CLEvent kernelEv  = imageOutputBufferEvent.getEvent();
                CLBuffer<Float>outputImageBuffer = (CLBuffer<Float>) imageOutputBufferEvent.getBuffer();


                Pointer<Float> outputImagePointer = Pointer.allocateFloats(1280 * 720 * 3);
                kernelEv.waitFor();
                CLEvent copyOutEv = outputImageBuffer.read(queue, outputImagePointer, false);
                ObjectCopyCL imagePointerEvent = new ObjectCopyCL(outputImagePointer, copyOutEv);
                return imagePointerEvent;
            }
        };

        NodeFunction writeImage = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                System.out.println("Write");
                ObjectCopyCL imageOutputPointerEvent = (ObjectCopyCL)in_operands[0];
                int numIter = (Integer)in_operands[1];
                Pointer<Float> outputImagePointer = (Pointer<Float>) imageOutputPointerEvent.getPointer();
                int imageWidth = (Integer) in_operands[2];
                int imageHeight = (Integer) in_operands[3];
                try {
                    SobelUtils sobelUtils = new SobelUtils();

                    CLEvent copyOutEv  = imageOutputPointerEvent.getEvent();
                    copyOutEv.waitFor();

                    sobelUtils.write(outputImagePointer, "imagemPathSphere_"+numIter+".ppm",
                            imageWidth, imageHeight);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }
        };

        int numWorkers = new Integer(args[0]);
        int numIters = new Integer(args[1]);
        int numSimultaneousIters = new Integer(args[2]);
        int maxNumSpheres = new Integer(args[3]);
        int outputImageWidth = new Integer(args[4]);
        int outputImageHeight = new Integer(args[5]);
        int maxScenesCreationSimultaneous = new Integer(args[6]);

        DFGraph dfg = new DFGraph();

        CLContext context = JavaCL.createBestContext(CLPlatform.DeviceFeature.GPU);
        Node contextFeeder = new Feeder(context);
        dfg.add(contextFeeder);

        List<Feeder> queueFeederList = new ArrayList<Feeder>();
        List<Feeder> inputBuferFeederList = new ArrayList<Feeder>();
        List<Feeder> outputBuferFeederList = new ArrayList<Feeder>();

        Feeder feederToken = new Feeder(new ObjectCopyCL());
        dfg.add(feederToken);

        Feeder imageWidthFeeder = new Feeder(outputImageWidth);
        dfg.add(imageWidthFeeder);

        Feeder imageHeightFeeder = new Feeder(outputImageHeight);
        dfg.add(imageHeightFeeder);

        for(int i = 0; i < numSimultaneousIters; i++) {
            CLQueue queue = context.createDefaultQueue();
            queueFeederList.add(new Feeder(queue));
            dfg.add(queueFeederList.get(i));
            //TODO check write of output to avoid errors with different spheres nnumbers
            Pointer<Float> imageInput = Pointer.allocateFloats(16 * maxNumSpheres);
            CLBuffer<Float> inputImageBuffer = context.createBuffer(CLMem.Usage.Input,
                    imageInput);
            inputBuferFeederList.add(new Feeder(inputImageBuffer));
            dfg.add(inputBuferFeederList.get(i));

            Pointer<Float> imageOutput = Pointer.allocateFloats(outputImageWidth * outputImageHeight * 3);
            CLBuffer<Float> outputImageBuffer = context.createBuffer(CLMem.Usage.Output, imageOutput);
            outputBuferFeederList.add(new Feeder(outputImageBuffer));
            dfg.add(outputBuferFeederList.get(i));
        }

        CLKernel render_kernel;
        String source = null;
        try {
            source = new Scanner(new File("pathTracerSphere.cl")).useDelimiter("\\Z").next();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        render_kernel = context.createProgram(source).createKernel("render_kernel");
        Feeder kernelFeeder = new Feeder(render_kernel);
        dfg.add(kernelFeeder);

        List<Node> configurateSceneNodes = new ArrayList<Node>();
        List<Node> copyScenesNodes = new ArrayList<Node>();
        List<Node> execKernelListNodes = new ArrayList<Node>();
        List<Node> copyOutListNodes = new ArrayList<Node>();
        List<Node> writeOutListNodes = new ArrayList<Node>();
        List<Feeder> numIterFeederList = new ArrayList<Feeder>();

        for(int i = 0; i<numIters ; i ++){
            //Add some Nodes
            Node configurateSceneNode =  new Node(configurationScene, 1);
            configurateSceneNodes.add(configurateSceneNode);
            dfg.add(configurateSceneNodes.get(i));

            Node copyScenesNode =  new Node(copySceneToGpu, 5);
            copyScenesNodes.add(copyScenesNode);
            dfg.add(copyScenesNodes.get(i));

            Node execKernelListNode =  new Node(executeKernel, 8);
            execKernelListNodes.add(execKernelListNode);
            dfg.add(execKernelListNodes.get(i));

            Node copyOutListNode =  new Node(copyOutSceneFromGpu, 2);
            copyOutListNodes.add(copyOutListNode);
            dfg.add(copyOutListNodes.get(i));

            Node writeOutListNode =  new Node(writeImage, 4);
            writeOutListNodes.add(writeOutListNode);
            dfg.add(writeOutListNodes.get(i));

            Feeder numIterFeeder = new Feeder(i);
            numIterFeederList.add(numIterFeeder);
            dfg.add(numIterFeederList.get(i));

            configurateSceneNodes.get(i).add_edge(copyScenesNodes.get(i), 0);
            inputBuferFeederList.get(i % numSimultaneousIters).add_edge(copyScenesNodes.get(i), 1);
            contextFeeder.add_edge(copyScenesNodes.get(i), 3);
            queueFeederList.get(i % numSimultaneousIters).add_edge(copyScenesNodes.get(i), 4);

            copyScenesNodes.get(i).add_edge(execKernelListNodes.get(i), 0);
            outputBuferFeederList.get(i % numSimultaneousIters).add_edge(execKernelListNodes.get(i), 1);
            kernelFeeder.add_edge(execKernelListNodes.get(i), 2);
            imageWidthFeeder.add_edge(execKernelListNodes.get(i), 3);
            imageHeightFeeder.add_edge(execKernelListNodes.get(i), 5);
            contextFeeder.add_edge(execKernelListNodes.get(i), 6);
            queueFeederList.get(i % numSimultaneousIters).add_edge(execKernelListNodes.get(i), 7);

            execKernelListNodes.get(i).add_edge(copyOutListNodes.get(i), 0);
            queueFeederList.get(i % numSimultaneousIters).add_edge(copyOutListNodes.get(i), 1);

            copyOutListNodes.get(i).add_edge(writeOutListNodes.get(i), 0);
            numIterFeederList.get(i).add_edge(writeOutListNodes.get(i), 1);
            imageWidthFeeder.add_edge(writeOutListNodes.get(i), 2);
            imageHeightFeeder.add_edge(writeOutListNodes.get(i), 3);

            if(i < numSimultaneousIters){
                feederToken.add_edge(copyScenesNodes.get(i), 2);
                feederToken.add_edge(execKernelListNodes.get(i), 4);
            }else{
                execKernelListNodes.get(i - numSimultaneousIters).add_edge(copyScenesNodes.get(i), 2);
                copyOutListNodes.get(i - numSimultaneousIters).add_edge(execKernelListNodes.get(i), 4);
            }

            if(i < maxScenesCreationSimultaneous){
                feederToken.add_edge(configurateSceneNodes.get(i), 0);
            }else{
                copyOutListNodes.get(i - maxScenesCreationSimultaneous).add_edge(configurateSceneNodes.get(i), 0);
            }
        }


        //start scheduler
        Scheduler sched = new Scheduler(dfg, numWorkers, false);
        sched.start();
    }
}
