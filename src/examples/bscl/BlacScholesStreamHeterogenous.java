package examples.bscl;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;


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
 * 
 * args baseFileName numBsclFiles numSimultaneousInstances numOptions
 * numOptionsFields numWorkers
 * 
 * /home/marcos/Dropbox/Mestrado/tese/experimentoBSCL/inputs/in_4 4 2 4 5 4
 * 
 * java -jar bscl.jar
 * /home/marcos/Dropbox/Mestrado/tese/experimentoBSCL/inputs/in_4 2 2 4 5 4
 *
 * 
 */
public class BlacScholesStreamHeterogenous {



	static CLContext context = JavaCL.createBestContext();
	static CLQueue queueCL1 = context.createDefaultQueue();
	static CLQueue queueCL2 = context.createDefaultQueue();
	static List<CLQueue> queueCLList = new ArrayList<CLQueue>();

	public static void main(String[] args) {

		for (int i = 0; i < args.length; i++)
			System.out.println("args[" + i + "]" + args[i]);
		/*
		// write your code here
		long startTime = System.currentTimeMillis();
		String baseFileName = args[0];
		int numBsclFiles = new Integer(args[1]);
		int numSimultaneousInstances = new Integer(args[2]);
		int numOptions = new Integer(args[3]);
		final int numOptionsFields = new Integer(args[4]);
		int numBsclNodes = new Integer(args[5]);
		float percentageWorkGPU = new Float(args[6]);
		int localWorkSizeGPU = new Integer(args[7]);
		int numWorkers = new Integer(args[8]);

		int numInstancesGPU = Math.round(numBsclFiles * percentageWorkGPU);
		int numInstancesCPU = numBsclFiles - numInstancesGPU;

		//initiateVariables(baseFileName, numBsclFiles, numSimultaneousInstances, numOptions, numOptionsFields,
		//		numInstancesCPU);
		*/

			NodeFunction readStockTime =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
					String stockTimeFile = (String) inputs[0];
					Integer indexBuffers = (Integer) inputs[1];
                    int numOptions = (Integer)inputs[2];

					// System.out.println("read stock time: " + indexBuffers);


					Pointer<Float> timesPointer  = Pointer.allocateFloats(numOptions);
					Pointer<Float> stocksPointer =  Pointer.allocateFloats(numOptions);
					int indexStockTime = 0;

					String line;

                    BufferedReader br = null;
                    FileReader fr = null;

                    // read file into stream, try-with-resources
                    try {
                        fr = new FileReader(stockTimeFile);
                        br = new BufferedReader(fr);

						while ((line = br.readLine()) != null) {
							String[] stockTimeFields = line.split(" ");
							Float s = new Float(stockTimeFields[0]);
							Float t = new Float(stockTimeFields[1]);

							stocksPointer.set(indexStockTime, s);
							timesPointer.set(indexStockTime, t);
							indexStockTime++;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

                    Object[] stockTimesPointers = new Object[]{stocksPointer, timesPointer};
					return indexBuffers;
				}
		};


			NodeFunction readStockTimeCPU =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {

					String stockTimeFile = (String) inputs[0];
					Integer numOptions = (Integer) inputs[1];

					Float[] times = new Float[numOptions];
					Float[] stocks = new Float[numOptions];
					int indexStockTime = 0;

					String[] stockTimeFields = null;
					Float s = null;
					Float t = null;


                    BufferedReader br = null;
                    FileReader fr = null;

					String line;
                    try {
                        fr = new FileReader(stockTimeFile);
                        br = new BufferedReader(fr);


						while ((line = br.readLine()) != null) {
							stockTimeFields = line.split(" ");
							// s = new Float(stockTimeFields[0]);
							s = Float.valueOf(stockTimeFields[0]);
							// t = new Float(stockTimeFields[1]);
							t = Float.valueOf(stockTimeFields[1]);

							stocks[indexStockTime] = s;
							times[indexStockTime] = t;
							indexStockTime++;
						}
					} catch (Exception e) {
						e.printStackTrace();
					}


					Object[] inputsBscl = new Object[]{stocks, times};

					return inputsBscl;
				}
		};

			NodeFunction readOptions =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {

					String optionsFile = (String) inputs[0];
                    int numOptions = (Integer)inputs[1];
                    int numFieldsBscl = (Integer)inputs[2];

					Pointer<Float> stocksPointer = Pointer.allocateFloats(numOptions);
					Pointer<Float> timesPointer = Pointer.allocateFloats(numOptions);
                    Pointer<Float> optionsPointer = Pointer.allocateFloats(numFieldsBscl * numOptions);

                    Float[] optionsCPU = new Float[numOptions];
                    Float[] stocksCPU = new Float[numOptions];
                    Float[] timesCPU = new Float[numOptions];

                    BufferedReader br = null;
                    FileReader fr = null;

                    String option;
                    int indexOptionsField = 0;
                    int indexStockTime = 0;
                    try {
                        fr = new FileReader(optionsFile);
                        br = new BufferedReader(fr);


                        while ((option = br.readLine()) != null) {
							String[] optionsFields = option.split(" ");

							Float s = new Float(optionsFields[0]);
							Float k = new Float(optionsFields[1]);
							Float rf = new Float(optionsFields[2]);
							Float v = new Float(optionsFields[4]);
							Float t = new Float(optionsFields[5]);
							Float div = new Float(optionsFields[7]);
							Float opt_p = new Float(optionsFields[8]);
							Float cp = optionsFields[6].equals("C") ? new Float(1.0) : new Float(-1.0);

							optionsPointer.set(indexOptionsField, k);
							optionsPointer.set(indexOptionsField + 1, rf);
							optionsPointer.set(indexOptionsField + 2, v);
							optionsPointer.set(indexOptionsField + 3, cp);
							optionsPointer.set(indexOptionsField + 4, div);

							optionsCPU[indexOptionsField] = k;
							optionsCPU[indexOptionsField + 1] = rf;
							optionsCPU[indexOptionsField + 2] = v;
							optionsCPU[indexOptionsField + 3] = cp;
							optionsCPU[indexOptionsField + 4] = div;

							stocksCPU[indexStockTime] = s;
							timesCPU[indexStockTime] = t;

							stocksPointer.set(indexStockTime, s);
							timesPointer.set(indexStockTime, t);

							indexOptionsField += 5;
							indexStockTime++;
						}

					} catch (IOException e) {
						e.printStackTrace();
					}

					Object[] inputsBscl = new Object[]{stocksCPU, timesCPU};

					return inputsBscl;
				}

		};


			NodeFunction assyncCopyOptionsIN =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
                    Pointer<Float> optionsPointer = (Pointer<Float>)inputs[0];
                    int numOptionsFields = (Integer)inputs[1];
                    int numOptions = (Integer)inputs[2];

                    CLBuffer<Float> bufferOptions = context.createBuffer(CLMem.Usage.Input,
                            Float.class, numOptionsFields * numOptions);

					CLEvent copyOptionsEv = bufferOptions.write(queueCL1, optionsPointer, false);
					return copyOptionsEv;
				}
		};


			NodeFunction assyncCopyIN =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {

					Object[] stockTimePointers = (Object[]) inputs[0];
                    int numOptions = (Integer) inputs[1];
                    CLContext context = (CLContext)inputs[2];
                    CLQueue queueCL = (CLQueue)inputs[3];

					Pointer<Float> stocksPointer = (Pointer<Float>)stockTimePointers[0];
					Pointer<Float> timesPointer = (Pointer<Float>)stockTimePointers[1];

					CLBuffer<Float> bufferStock = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);
					CLBuffer<Float> bufferTime = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);

                    CLEvent kernelEv = (CLEvent)inputs[4];
					if (kernelEv != null)
						kernelEv.waitFor();

                    CLEvent copyStocksEv = bufferStock.write(queueCL, stocksPointer, false);
					CLEvent copyTimeEv = bufferTime.write(queueCL, timesPointer, false);

                    Object[] copyEvents = new Object[]{copyStocksEv, copyTimeEv};
					return copyEvents;
				}

		};


			NodeFunction assyncKernel =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {

					String source = "";
					//Integer indexBuffers = (Integer) inputs[0];

					try {
                        source = new Scanner(new File("kernelBscl.cl")).useDelimiter("\\Z").next();
					} catch (IOException e) {
						e.printStackTrace();
					}

					//if (kernel == null)
					//	kernel = context.createProgram(source).createKernel("bscl");

                    CLKernel kernel = context.createProgram(source).createKernel("bscl");

                    Object[] bufferEventOptions = (Object[]) inputs[0];
                    CLBuffer<Float> bufferOptions = (CLBuffer<Float>)bufferEventOptions[0];
                    CLEvent copyOptionsEv = (CLEvent)bufferEventOptions[1];

                    Object[] buffersStockTimeEvents = (Object[])inputs[1];
					CLBuffer<Float> bufferStock = (CLBuffer<Float>)buffersStockTimeEvents[0];
					CLBuffer<Float> bufferTime = (CLBuffer<Float>)buffersStockTimeEvents[1];

                    int numOptions = (Integer)inputs[2];

					CLBuffer<Float> bufferOutput = context.createBuffer(CLMem.Usage.Input, Float.class, numOptions);;

					kernel.setArgs(5, bufferOptions, bufferStock, bufferTime, bufferOutput);


					copyOptionsEv.waitFor();

					CLEvent copyStocksEv = (CLEvent)buffersStockTimeEvents[2];
					copyStocksEv.waitFor();
					CLEvent copyTimeEv = (CLEvent)buffersStockTimeEvents[3];
					copyTimeEv.waitFor();

					 CLQueue queueCL = (CLQueue)inputs[3];
					//CLQueue queueCL = queueCL1;

					CLEvent kernelEv = null;
					// kernelEv = kernel.enqueueNDRange(queueCL, new int[] { numOptions
					// });

                    int localWorkSizeGPU = (Integer)inputs[4];
					int[] localWorkSizes = new int[]{localWorkSizeGPU, 1};
					int globalSize = ((int) ((float) numOptions / (float) localWorkSizes[0]) * localWorkSizes[0]);
					int[] globalWorkSizes = null;
					if (globalSize <= 0) {
						globalSize = numOptions;
						globalWorkSizes = new int[]{globalSize};
						kernelEv = kernel.enqueueNDRange(queueCL, globalWorkSizes);
					} else {
						globalWorkSizes = new int[]{globalSize, 1};
						kernelEv = kernel.enqueueNDRange(queueCL, globalWorkSizes, localWorkSizes);
					}

					//kernelEventsList.set(indexBuffers, kernelEv);
					// queueCL.finish();

					// sm.put("Porra","Kcete");*/
                    Object[] kernelOutputEvent = new Object[]{bufferOutput, kernelEv};
					return kernelOutputEvent;
				}
		};


			NodeFunction bscl =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
			Object[] inputsBscl = (Object[]) inputs[0];
			Float[] a_g = (Float[]) inputs[1];

			// for(int i=0;i<optionsCPU.length;i++)
			// System.out.println("optionsCPU: " + optionsCPU[i]);
			Float[] s_g = (Float[])inputsBscl[0];
			Float[] t_g = (Float[])inputsBscl[1];

			int threadId = (Integer) inputs[1];
			int numOptions = (Integer) inputs[2];
			int numBsclNodes = (Integer) inputs[3];
			int numOptionsFields = (Integer) inputs[4];
			int chunck = numOptions / numBsclNodes;
			Float[] res_g = new Float[chunck];

			int start = threadId * chunck;
			int end = start + chunck < numOptions ? start + chunck : numOptions;

			for (int i = start, indexOutput = 0; i < end; i++, indexOutput++) {
				System.out.println("i:"+i);
				float s = s_g[i];
				// System.out.println("s bscl CPU"+ s);
				float k = a_g[(i * numOptionsFields) + 0];
				float rf = a_g[(i * numOptionsFields) + 1];
				float v = a_g[(i * numOptionsFields) + 2];
				float t = t_g[i];
				// printf("t kernel:%f\n", t);
				float cp = a_g[(i * numOptionsFields) + 3];
				float div = a_g[(i * numOptionsFields) + 4];

				float optprice = 0;

				float d1 = ((float) Math.log(s / k) + ((rf - div) + (0.5f * ((float) Math.pow(v, 2)))) * t)
						/ (v * (float) Math.sqrt(t));

				float d2 = d1 - v * (float) Math.sqrt(t);

				float cp_d1 = cp * d1;
				float cp_d2 = cp * d2;
				//optprice = (cp * s * (float) Math.exp(-div * t) * cdf(cp_d1))
				//		- (cp * k * (float) Math.exp(-rf * t) * cdf(cp_d2));

				res_g[indexOutput] = optprice;
			}
			return res_g;

		};

		/*
		 * NodeFunction assyncCopyOut = (NodeFunction & Serializable) (Object[]
		 * inputs) -> { Integer indexBuffers = (Integer) inputs[0];
		 * System.out.println("assyncCopyOut: " + indexBuffers); CLEvent
		 * kernelEv = kernelEventsList.get(indexBuffers); kernelEv.waitFor();
		 * CLBuffer<Float> bufferOuput = bufferOutputList.get(indexBuffers);
		 * 
		 * Pointer<Float> optionsPrice = bufferOuput.read(queueCL); for (long i
		 * = 0, numEle = optionsPrice.getValidElements(); i < numEle; i++)
		 * System.out.println("optionsPrice.get " + i + ":" +
		 * optionsPrice.get(i));
		 * 
		 * return 0; };
		 */


			NodeFunction assyncCopyOut =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
                    Object[] outBufferKernelEv = (Object[]) inputs[0];

				// System.out.println("assyncCopyOut: " + indexBuffers);
				CLEvent kernelEv = (CLEvent)outBufferKernelEv[1];
				kernelEv.waitFor();
				CLBuffer<Float> bufferOuput = (CLBuffer<Float>)outBufferKernelEv[0];

				CLQueue queueCL = (CLQueue)inputs[1];
                int numOptions = (Integer)inputs[2];
				// Pointer<Float> optionsPrice = bufferOuput.read(queueCL);

				Pointer<Float> optionsPrice =  Pointer.allocateFloats(numOptions);
                CLEvent copyOutEv = bufferOuput.read(queueCL, optionsPrice, false);
				Object[] pointerEvent = new Object[]{optionsPrice, copyOutEv};

				return pointerEvent;
			}
		};


			NodeFunction writeResults =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {


					Object[] pointerEvent  = (Object[]) inputs[0];
					Integer numFile = (Integer) inputs[1];

					Pointer<Float> optionsPrice = (Pointer<Float>)pointerEvent[0];
					CLEvent copyOutEv = (CLEvent)pointerEvent[1];
					copyOutEv.waitFor();

					try{
                        PrintWriter writer = new PrintWriter("outputs/out_" + numFile + ".txt", "UTF-8");
						for (long i = 0, numEle = optionsPrice.getValidElements(); i < numEle; i++)
							writer.write(optionsPrice.get(i) + "\n");

					} catch (Exception e)

					{
						e.printStackTrace();
					}

					return 0;
				}
		};



			NodeFunction writeOptions =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {
					int numOutputFile = (Integer) inputs[inputs.length - 1];
					// Path path = Paths.get("o*()utputs/out_" + numFile + ".txt");//

                    try{
                    PrintWriter writer = new PrintWriter("outputs/out_" + numOutputFile + ".txt", "UTF-8");

						for (int i = 0; i < inputs.length - 1; i++) {
							Float[] optionsPrice = (Float[]) inputs[i];
							for (int j = 0; j < optionsPrice.length; j++) {
								// System.out.println("optionsPrice: " + ((i *
								// optionsPrice.length) + j) + " " + optionsPrice[j]);
								writer.write(optionsPrice[j] + "\n");
							}
						}
					} catch (Exception e)

					{
						e.printStackTrace();
					}

					return 0;
				}
		};

				/*
		DFGraph dfg = new DFGraph();
		// System.out.println("Testando...");
		if (percentageWorkGPU > 0.0)

		{
			Feeder optionsFileNode = new Feeder(baseFileName + ".txt");
			Node readNode = new Node(readOptions, 1);
			Feeder feeder0 = new Feeder(0);
			Node assyncCopyOptionsInNode = new Node(assyncCopyOptionsIN, 1);
			Node assyncCopyInNode = new Node(assyncCopyIN, 2);
			Node assyncKernelNode = new Node(assyncKernel, 2);
			Feeder feederOut0 = new Feeder(0);
			Node assyncCopyOutNode = new Node(assyncCopyOut, 1);
			Node writeResults0 = new Node(writeResults, 2);

			Feeder feederStockTimeNode = new Feeder(baseFileName + "_1.txt");
			Node readStockTimeNode = new Node(readStockTime, 2);
			Feeder feeder1 = new Feeder(1);
			Node assyncCopyInNode2 = new Node(assyncCopyIN, 2);
			Node assyncKernelNode2 = new Node(assyncKernel, 1);
			Feeder feederOut1 = new Feeder(1);
			Node assyncCopyOutNode2 = new Node(assyncCopyOut, 1);
			Node writeResults1 = new Node(writeResults, 2);

			List<Node> assyncKernelNodeList = new ArrayList<>();
			assyncKernelNodeList.add(assyncKernelNode);
			assyncKernelNodeList.add(assyncKernelNode2);

			List<Node> assyncCopyOutNodesList = new ArrayList<>();
			assyncCopyOutNodesList.add(assyncCopyOutNode);
			assyncCopyOutNodesList.add(assyncCopyOutNode2);

			List<Node> writeResultsNodesList = new ArrayList<>();
			writeResultsNodesList.add(writeResults0);
			writeResultsNodesList.add(writeResults1);

			dfg.add(optionsFileNode);
			dfg.add(readNode);
			dfg.add(feeder0);
			dfg.add(assyncCopyOptionsInNode);
			dfg.add(assyncCopyInNode);
			dfg.add(assyncKernelNodeList.get(0));
			dfg.add(feederOut0);
			dfg.add(assyncCopyOutNodesList.get(0));
			dfg.add(writeResultsNodesList.get(0));

			dfg.add(feederStockTimeNode);
			dfg.add(feeder1);
			dfg.add(readStockTimeNode);
			dfg.add(assyncCopyInNode2);
			dfg.add(assyncKernelNodeList.get(1));
			dfg.add(feederOut1);
			dfg.add(assyncCopyOutNode2);
			dfg.add(assyncCopyOutNodesList.get(1));
			dfg.add(writeResultsNodesList.get(1));

			// System.out.println("assyncCopyOutNode: " + assyncCopyOutNode.id);
			// System.out.println("assyncCopyOutNodesList.get(0).id: " +
			// assyncCopyOutNodesList.get(0).id);

			// int numBsclFiles = 4;
			// int numSimultaneousInstances = 2;
			// String baseFileName = "in_4_";

			List<Feeder> feederStockTimeNodeList = new ArrayList<>();
			List<Node> readStockTimeNodeList = new ArrayList<>();
			List<Feeder> feeder1List = new ArrayList<>();
			List<Node> assyncCopyInNode2List = new ArrayList<>();
			// List<Node> assyncKernelNode2List = new ArrayList<>();
			List<Feeder> feederNumOutList = new ArrayList<>();

			for (int i = numSimultaneousInstances; i < numBsclFiles; i++) {
				feederStockTimeNodeList.add(new Feeder(baseFileName + "_" + i + ".txt"));
				readStockTimeNodeList.add(new Node(readStockTime, 3));
				feeder1List.add(new Feeder(i % 2 == 0 ? 0 : 1));
				assyncCopyInNode2List.add(new Node(assyncCopyIN, 2));
				assyncKernelNodeList.add(new Node(assyncKernel, 2));
				feederNumOutList.add(new Feeder(i));
				assyncCopyOutNodesList.add(new Node(assyncCopyOut, 1));
				writeResultsNodesList.add(new Node(writeResults, 2));

				// System.out.println("assyncCopyOutNodesList.size():" +
				// assyncCopyOutNodesList.size());

				int indLists = i - numSimultaneousInstances;

				dfg.add(feederStockTimeNodeList.get(indLists));
				dfg.add(feeder1List.get(indLists));
				dfg.add(readStockTimeNodeList.get(indLists));
				dfg.add(assyncCopyInNode2List.get(indLists));
				dfg.add(assyncKernelNodeList.get(i));
				dfg.add(feederNumOutList.get(indLists));
				dfg.add(assyncCopyOutNodesList.get(i));
				dfg.add(writeResultsNodesList.get(i));
			*/
				/*
				 * System.out.println("i:" + i);
				 * System.out.println("assyncCopyOutNodesList.get(i).id:" +
				 * assyncCopyOutNodesList.get(i).id);
				 * System.out.println("assyncKernelNode2List.get(indLists).id:"
				 * + assyncKernelNode2List.get(indLists).id);
				 */
/*
				feederStockTimeNodeList.get(indLists).add_edge(readStockTimeNodeList.get(indLists), 0);
				feeder1List.get(indLists).add_edge(readStockTimeNodeList.get(indLists), 1);
				readStockTimeNodeList.get(indLists).add_edge(assyncCopyInNode2List.get(indLists), 0);
				feeder1List.get(indLists).add_edge(assyncCopyInNode2List.get(indLists), 1);
				assyncKernelNodeList.get(indLists).add_edge(readStockTimeNodeList.get(indLists), 2);
				assyncCopyInNode2List.get(indLists).add_edge(assyncKernelNodeList.get(i), 0);
				assyncCopyOutNodesList.get(indLists).add_edge(assyncKernelNodeList.get(i), 1);
				assyncKernelNodeList.get(i).add_edge(assyncCopyOutNodesList.get(i), 0);
				assyncCopyOutNodesList.get(i).add_edge(writeResultsNodesList.get(i), 0);
				feederNumOutList.get(indLists).add_edge(writeResultsNodesList.get(i), 1);

			}

			optionsFileNode.add_edge(readNode, 0);
			readNode.add_edge(assyncCopyInNode, 1);
			readNode.add_edge(assyncCopyOptionsInNode, 0);
			feeder0.add_edge(assyncCopyInNode, 0);
			assyncCopyInNode.add_edge(assyncKernelNode, 0);
			assyncCopyOptionsInNode.add_edge(assyncKernelNode, 1);
			assyncKernelNode.add_edge(assyncCopyOutNode, 0);
			assyncCopyOutNode.add_edge(writeResults0, 0);
			feederOut0.add_edge(writeResults0, 1);

			feederStockTimeNode.add_edge(readStockTimeNode, 0);
			feeder1.add_edge(readStockTimeNode, 1);
			readStockTimeNode.add_edge(assyncCopyInNode2, 0);
			feeder1.add_edge(assyncCopyInNode2, 1);
			assyncCopyInNode2.add_edge(assyncKernelNode2, 0);
			// assyncCopyOutNode.add_edge(assyncKernelNode2, 1);
			assyncKernelNode2.add_edge(assyncCopyOutNode2, 0);
			assyncCopyOutNode2.add_edge(writeResults1, 0);
			feederOut1.add_edge(writeResults1, 1);
		}
		// ------------------CPU WORK-------------------------
		// Feeder optionsFileNode = new Feeder(baseFileName + ".txt");
		// Node readNodeCPU = new Node(readOptionsCPU, 1);

		List<Node> readNodeCPUNodeList = new ArrayList<>();
		List<Feeder> numOutFileNodeList = new ArrayList<>();
		List<Node> writeOptNodeList = new ArrayList<>();
		List<Feeder> feederIndexBufferList = new ArrayList<>();
		List<Feeder> feederStockTimeCPUNodeList = new ArrayList<>();

		for (int indexInstanceCPU = 0, indexIteration = numInstancesGPU; indexInstanceCPU < numInstancesCPU; indexInstanceCPU++, indexIteration++)

		{
			// baseFileName + "_" + i + ".txt"

			numOutFileNodeList.add(new Feeder(indexIteration));
			writeOptNodeList.add(new Node(writeOptions, numBsclNodes + 1));
			feederIndexBufferList.add(new Feeder(indexInstanceCPU));
			if (indexIteration != 0) {
				readNodeCPUNodeList.add(new Node(readStockTimeCPU, 2));
				feederStockTimeCPUNodeList.add(new Feeder(baseFileName + "_" + indexIteration + ".txt"));
			} else {
				readNodeCPUNodeList.add(new Node(readOptions, 2));
				feederStockTimeCPUNodeList.add(new Feeder(baseFileName + ".txt"));
			}
			// dfg.add(optionsFileNode);
			dfg.add(readNodeCPUNodeList.get(indexInstanceCPU));
			dfg.add(numOutFileNodeList.get(indexInstanceCPU));
			dfg.add(writeOptNodeList.get(indexInstanceCPU));
			dfg.add(feederIndexBufferList.get(indexInstanceCPU));
			dfg.add(feederStockTimeCPUNodeList.get(indexInstanceCPU));

			feederStockTimeCPUNodeList.get(indexInstanceCPU).add_edge(readNodeCPUNodeList.get(indexInstanceCPU), 0);
			feederIndexBufferList.get(indexInstanceCPU).add_edge(readNodeCPUNodeList.get(indexInstanceCPU), 1);
			numOutFileNodeList.get(indexInstanceCPU).add_edge(writeOptNodeList.get(indexInstanceCPU), numBsclNodes);

			List<Feeder> listaFeeders = new ArrayList<>();
			List<Node> listaBsclNodes = new ArrayList<>();

			for (int i = 0; i < numBsclNodes; i++) {
				listaFeeders.add(new Feeder(i));
				dfg.add(listaFeeders.get(i));

				listaBsclNodes.add(new Node(bscl, 2));
				dfg.add(listaBsclNodes.get(i));

				readNodeCPUNodeList.get(indexInstanceCPU).add_edge(listaBsclNodes.get(i), 0);
				listaFeeders.get(i).add_edge(listaBsclNodes.get(i), 1);

				listaBsclNodes.get(i).add_edge(writeOptNodeList.get(indexInstanceCPU), i);
			}
		}
		// ------------------CPU WORK-------------------------

		//generateGraphVizDot(dfg);
		Scheduler sched = new Scheduler(dfg, numWorkers, false);
		sched.start();
		long endTime = System.currentTimeMillis();
		long totalTime = endTime - startTime;
		System.out.println("Total Time: " + totalTime);
	}

	private static void initiateVariables(String baseFileName, int numBsclFiles, int numSimultaneousInstances,
			int numOptions2, int numOptionsFields, int numInstancesCPU) {

		queueCLList.add(queueCL1);
		queueCLList.add(queueCL2);


        /*
		stocksCPUList = new ArrayList<>();
		timesCPUList = new ArrayList<>();
		for (int i = 0; i < numInstancesCPU; i++) {
			// indexReadOptionsCPUList.add(0);
			stocksCPUList.add(new Float[numOptions]);
			timesCPUList.add(new Float[numOptions]);
		}*/

	};
/*
	static float cdf(float x) {

		// constants cdf
		float a1 = 0.254829592f;
		float a2 = -0.284496736f;
		float a3 = 1.421413741f;
		float a4 = -1.453152027f;
		float a5 = 1.061405429f;
		float p = 0.3275911f;

		// sign x
		int sign = 1;
		sign = x >= 0 ? 1 : -1;

		x = (float) (Math.abs(x) / Math.sqrt(2.0f));

		// A&S formula 7.1.26
		float t = 1.0f / (1.0f + p * x);
		float y = 1.0f - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * (float) Math.exp(-x * x);

		float cdf = 0.5f * (1.0f + sign * y);

		return cdf;
	}

*/}
}