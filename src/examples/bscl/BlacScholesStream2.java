package examples.bscl;

import java.io.*;


import java.util.Scanner;


import jsucuriinoserialize.*;
import org.bridj.Pointer;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLMem;
import com.nativelibs4java.opencl.CLQueue;
import com.nativelibs4java.opencl.JavaCL;

/**
 * Created by marcos on 22/10/16.
 */
public class BlacScholesStream2 {
	static CLContext context = null;
	static CLQueue queueCL = null;

	// TODO colocar inicializacao em alguma funcao com os valores de tamanho
	// dinamicos

	public static void main(String[] args) {
		// write your code here


			NodeFunction readStockTime =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
					String stockTimeFile = (String) inputs[0];

					Pointer<Float> stocksPointer = Pointer.allocateFloats(4);
					Pointer<Float> timesPointer = Pointer.allocateFloats(4);

					BufferedReader br = null;
					FileReader fr = null;

					// read file into stream, try-with-resources
					try {
						fr = new FileReader(stockTimeFile);
						br = new BufferedReader(fr);
						String option;
						while((option = br.readLine()) != null){
							String[] stockTimeFields = option.split(" ");
							Float s = new Float(stockTimeFields[0]);
							Float t = new Float(stockTimeFields[1]);

							stocksPointer.set(s);
							timesPointer.set(t);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					return 0;
				}
		};


			NodeFunction readOptions =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
					String optionsFile = (String) inputs[0];
                    int numOptions = (Integer)inputs[1];
					BufferedReader br = null;
					FileReader fr = null;

					Pointer<Float> stocksPointer = Pointer.allocateFloats(numOptions);
					Pointer<Float> timesPointer = Pointer.allocateFloats(numOptions);
                    Pointer<Float> optionsPointer = Pointer.allocateFloats(5 * numOptions);

					// read file into stream, try-with-resources
					try {
						fr = new FileReader(optionsFile);
						br = new BufferedReader(fr);
						String option;
                        int indexOpt = 0;
                        int indexStockTimes = 0;
						while((option = br.readLine()) != null){
							String[] optionsFields = option.split(" ");
							System.out.println(optionsFields[1]);

							Float s = new Float(optionsFields[0]);
							Float k = new Float(optionsFields[1]);
							Float rf = new Float(optionsFields[2]);
							Float v = new Float(optionsFields[4]);
							Float t = new Float(optionsFields[5]);
							Float div = new Float(optionsFields[7]);
							Float opt_p = new Float(optionsFields[8]);
							Float cp = optionsFields[6].equals("C") ? new Float(1.0) : new Float(-1.0);

							optionsPointer.set(indexOpt, k);
							optionsPointer.set(indexOpt + 1, rf);
							optionsPointer.set(indexOpt + 2, v);
							optionsPointer.set(indexOpt + 3, cp);
							optionsPointer.set(indexOpt + 4, div);

							stocksPointer.set(indexStockTimes, s);
							timesPointer.set(indexStockTimes, t);

                            indexOpt += 5;
                            indexStockTimes++;
						}

					} catch (IOException e) {
						e.printStackTrace();
					}
                    imprimirPointerList(optionsPointer);
                    Object[] inputsBuffers = new Object[]{optionsPointer, stocksPointer, timesPointer};
					return inputsBuffers;
				}

		};


			NodeFunction assyncCopyIN =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
					Object[] buffersInput = (Object[])inputs[0];
                    Pointer<Float> optionsPointer = (Pointer<Float>)buffersInput[0];
                    Pointer<Float> stocksPointer = (Pointer<Float>)buffersInput[1];
                    Pointer<Float> timesPointer = (Pointer<Float>)buffersInput[2];

                    int numOptions = (Integer)inputs[1];

                    CLBuffer<Float> bufferOptions = context.createBuffer(CLMem.Usage.Input, Float.class, 5 * numOptions);
					CLBuffer<Float> bufferStock = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);
					CLBuffer<Float> bufferTime = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);

					CLEvent copyOptionsEv = bufferOptions.write(queueCL, optionsPointer, false);
					CLEvent copyStocksEv = bufferStock.write(queueCL, stocksPointer, false);
					CLEvent copyTimeEv = bufferTime.write(queueCL, timesPointer, false);

                    Object[] bufferEvents = new Object[]{bufferOptions, bufferStock, bufferTime,
                            copyOptionsEv, copyStocksEv, copyTimeEv};

                    return bufferEvents;
				}

		};


			NodeFunction assyncKernel =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
					String source = "";

					try {
						source = new Scanner(new File("kernelBscl.cl")).useDelimiter("\\Z").next();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

                    Object[] buffersEvents = (Object[])inputs[0];
                    CLBuffer<Float> bufferOptions = (CLBuffer<Float>)buffersEvents[0];
                    CLBuffer<Float> bufferStock = (CLBuffer<Float>)buffersEvents[1];
                    CLBuffer<Float> bufferTime = (CLBuffer<Float>)buffersEvents[2];


                    int numOptions = (Integer)inputs[1];
                    CLBuffer<Float> bufferOutput = context.createBuffer(CLMem.Usage.Output, Float.class, numOptions);

                    CLKernel kernel = context.createProgram(source).createKernel("bscl");
                    kernel.setArgs(5, bufferOptions, bufferStock, bufferTime, bufferOutput);

                    CLEvent copyOptionsEv = (CLEvent)buffersEvents[3];
                    CLEvent copyStocksEv = (CLEvent)buffersEvents[4];
                    CLEvent copyTimeEv = (CLEvent)buffersEvents[5];;

                    copyOptionsEv.waitFor();
                    copyStocksEv.waitFor();
                    copyTimeEv.waitFor();

                    CLEvent kernelEv = kernel.enqueueNDRange(queueCL, new int[]{4});
                    queueCL.finish();

                    Object[] outputEvent = new Object[]{bufferOutput, kernelEv};
					return outputEvent;
				}


		};

			NodeFunction assyncCopyOut =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
                    Object[] outputEvent = (Object[])inputs[0];
                    CLBuffer<Float> bufferOutput = (CLBuffer<Float>) outputEvent[0];
                    CLEvent kernelEv = (CLEvent)outputEvent[1];
                    int numOptions = (Integer)inputs[1];
                    kernelEv.waitFor();

					Pointer<Float> optionsPrice = bufferOutput.read(queueCL);
					for (long i = 0 ; i < numOptions; i++)
						System.out.println("optionsPrice.get " + i + ":" + optionsPrice.get(i));

					return 0;
				}
		};

        String baseFileName = args[0];
        int numOptions = new Integer(args[1]);

        initializeCLVariables();

		DFGraph dfg = new DFGraph();
		// System.out.println("Testando...");

		Feeder optionsFileNode = new Feeder(baseFileName+".txt");
		Node readNode = new Node(readOptions, 2);
		Node assyncCopyInNode = new Node(assyncCopyIN, 2);
		Node assyncKernelNode = new Node(assyncKernel, 2);
		Node assyncCopyOutNode = new Node(assyncCopyOut, 2);
        Feeder numOptionsFeeder = new Feeder(numOptions);

		dfg.add(optionsFileNode);
		dfg.add(readNode);
		dfg.add(assyncCopyInNode);
		dfg.add(assyncKernelNode);
		dfg.add(assyncCopyOutNode);
        dfg.add(numOptionsFeeder);

		optionsFileNode.add_edge(readNode, 0);
        numOptionsFeeder.add_edge(readNode, 1);
		readNode.add_edge(assyncCopyInNode, 0);
        numOptionsFeeder.add_edge(assyncCopyInNode, 1);
		assyncCopyInNode.add_edge(assyncKernelNode, 0);
        numOptionsFeeder.add_edge(assyncKernelNode, 1);
		assyncKernelNode.add_edge(assyncCopyOutNode, 0);
        numOptionsFeeder.add_edge(assyncCopyOutNode, 1);

		Scheduler sched = new Scheduler(dfg, 3, false);
		sched.start();

	}

    static private void initializeCLVariables(){
          context = JavaCL.createBestContext();
          queueCL = context.createDefaultQueue();
    }

	private static void imprimirPointerList(Pointer<Float> optionsPointer) {
		for (long i = 0, numEle = optionsPointer.getValidElements(); i < numEle; i++)
			System.out.println("optionsPointer.get " + i + ":" + optionsPointer.get(i));

	}
}