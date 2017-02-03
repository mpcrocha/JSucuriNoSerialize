package examples.bscl;

import jsucuriinoserialize.*;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

// in_4.txt 4
public class BSCLJSucuri {

	static int indexOptionsField = 0;
	static int indexStockTime = 0;

	public static void main(String[] args) {


			NodeFunction readOptions =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {

					String optionsFile = (String) inputs[0];
					Float[] options = new Float[20];
					Float[] stocks = new Float[4];
					Float[] times = new Float[4];

					BufferedReader br = null;
					FileReader fr = null;

					// read file into stream, try-with-resources
					try {
						fr = new FileReader(optionsFile);
						br = new BufferedReader(fr);
						String option;
						while((option = br.readLine()) != null){
							String[] optionsFields = option.split(" ");
							// System.out.println(optionsFields[1]);

							Float s = new Float(optionsFields[0]);
							Float k = new Float(optionsFields[1]);
							Float rf = new Float(optionsFields[2]);
							Float v = new Float(optionsFields[4]);
							Float t = new Float(optionsFields[5]);
							Float div = new Float(optionsFields[7]);
							Float opt_p = new Float(optionsFields[8]);
							Float cp = optionsFields[6].equals("C") ? new Float(1.0) : new Float(-1.0);

							options[indexOptionsField] = k;
							options[indexOptionsField + 1] = rf;
							options[indexOptionsField + 2] = v;
							options[indexOptionsField + 3] = cp;
							options[indexOptionsField + 4] = div;

							stocks[indexStockTime] = s;
							times[indexStockTime] = t;

							indexOptionsField += 5;
							indexStockTime++;
						};
						// stocksPointersList.set(0, stocksPointer);
						// timesPointersList.set(0, timesPointer);

					} catch (IOException e) {
						e.printStackTrace();
					}
					List<Float[]> inputsBscl = new ArrayList<Float[]>();
					inputsBscl.add(options);
					inputsBscl.add(stocks);
					inputsBscl.add(times);
					return inputsBscl;
				}
		};

		NodeFunction bscl =  new NodeFunction() {
			@Override
			public Object f(Object[] inputs) {

				List<Float[]> inputsBscl = (List<Float[]>) inputs[0];
				Float[] a_g = inputsBscl.get(0);
				Float[] s_g = inputsBscl.get(1);
				Float[] t_g = inputsBscl.get(2);

				int threadId = (Integer) inputs[1];
				int numOpts = 4;
				int numBsclNodes = 2;
				int dim = 5;
				int chunck = numOpts / numBsclNodes;
				Float[] res_g = new Float[chunck];

				int start = threadId * chunck;
				int end = start + chunck < numOpts ? start + chunck : numOpts;

				for (int i = start, indexOutput = 0; i < end; i++, indexOutput++) {
					float s = s_g[i];
					// printf("s kernel:%f\n", s);
					float k = a_g[(i * dim) + 0];
					float rf = a_g[(i * dim) + 1];
					float v = a_g[(i * dim) + 2];
					float t = t_g[i];
					// printf("t kernel:%f\n", t);
					float cp = a_g[(i * dim) + 3];
					float div = a_g[(i * dim) + 4];

					int numRepetions = 10000;

					float optprice = 0;
					for (int indexRepKernel = 0; indexRepKernel < numRepetions; indexRepKernel++) {
						float d1 = ((float) Math.log(s / k) + ((rf - div) + (0.5f * ((float) Math.pow(v, 2)))) * t)
								/ (v * (float) Math.sqrt(t));

						float d2 = d1 - v * (float) Math.sqrt(t);

						float cp_d1 = cp * d1;
						float cp_d2 = cp * d2;
						optprice = (cp * s * (float) Math.exp(-div * t) * cdf(cp_d1))
								- (cp * k * (float) Math.exp(-rf * t) * cdf(cp_d2));
					}

					res_g[indexOutput] = optprice;
				}
				return res_g;
			}
		};


			NodeFunction writeOptions =  new NodeFunction() {
				@Override
				public Object f(Object[] inputs) {

					int numOutputFile = (Integer) inputs[inputs.length - 1];
					// Path path = Paths.get("o*()utputs/out_" + numFile + ".txt");//
					//Path path = Paths.get();//
					try{
						PrintWriter writer = new PrintWriter("outputs/out_" + numOutputFile + ".txt", "UTF-8");

						for (int i = 0; i < inputs.length - 1; i++) {
							Float[] optionsPrice = (Float[]) inputs[i];
							for (int j = 0; j < optionsPrice.length; j++) {
								System.out.println("optionsPrice: " + ((i * optionsPrice.length) + j) + " " + optionsPrice[j]);
								writer.write(optionsPrice[j] + "\n");
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					return 0;
				}
		};

		String baseFileName = args[0];
		int numBSCLNodes = new Integer(args[1]);

		DFGraph dfg = new DFGraph();
		
		Feeder optionsFileNode = new Feeder(baseFileName + ".txt");
		Node readNode = new Node(readOptions, 1);
		Feeder numOutFileNode = new Feeder(0);
		Node writeOptNode = new Node(writeOptions, numBSCLNodes+1);
		
		dfg.add(optionsFileNode);
		dfg.add(readNode);
		dfg.add(numOutFileNode);
		dfg.add(writeOptNode);

		optionsFileNode.add_edge(readNode, 0);
		numOutFileNode.add_edge(writeOptNode, numBSCLNodes);

		List<Feeder> listaFeeders = new ArrayList<Feeder>();
		List<Node> listaBsclNodes = new ArrayList<Node>();

		for (int i = 0; i < numBSCLNodes; i++) {
			listaFeeders.add(new Feeder(i));
			dfg.add(listaFeeders.get(i));

			listaBsclNodes.add(new Node(bscl, 2));
			dfg.add(listaBsclNodes.get(i));

			readNode.add_edge(listaBsclNodes.get(i), 0);
			listaFeeders.get(i).add_edge(listaBsclNodes.get(i), 1);

			listaBsclNodes.get(i).add_edge(writeOptNode, i);
		}

		Scheduler sched = new Scheduler(dfg, 3, false);
		sched.start();

	}

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
}
