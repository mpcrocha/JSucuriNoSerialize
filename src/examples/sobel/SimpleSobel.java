package examples.sobel;
import com.nativelibs4java.opencl.CLAbstractUserProgram;
import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLBuildException;
import com.nativelibs4java.opencl.CLContext;
import com.nativelibs4java.opencl.CLEvent;
import com.nativelibs4java.opencl.CLImage2D;
import com.nativelibs4java.opencl.CLKernel;
import com.nativelibs4java.opencl.CLProgram;
import com.nativelibs4java.opencl.CLQueue;
import java.io.IOException;
/** Wrapper around the OpenCL program SimpleSobel */
public class SimpleSobel extends CLAbstractUserProgram {
	public SimpleSobel(CLContext context) throws IOException {
		super(context, readRawSourceForClass(SimpleSobel.class));
	}
	public SimpleSobel(CLProgram program) throws IOException {
		super(program, readRawSourceForClass(SimpleSobel.class));
	}
	CLKernel simpleSobel_kernel;
	public synchronized CLEvent simpleSobel(CLQueue commandQueue, CLImage2D input, int width, int height,
											CLBuffer<Float > gradientOutput, CLBuffer<Float > directionOutput,
											int globalWorkSizes[], int localWorkSizes[], CLEvent... eventsToWaitFor)
			throws CLBuildException {

		if ((simpleSobel_kernel == null)) 
			simpleSobel_kernel = createKernel("simpleSobel");
		simpleSobel_kernel.setArgs(input, width, height, gradientOutput, directionOutput);
		return simpleSobel_kernel.enqueueNDRange(commandQueue, globalWorkSizes, localWorkSizes, eventsToWaitFor);
	}
	CLKernel normalizeImage_kernel;
	public synchronized CLEvent normalizeImage(CLQueue commandQueue, CLBuffer<Float > input, float maxValue, CLBuffer<Byte > output, int globalWorkSizes[], int localWorkSizes[], CLEvent... eventsToWaitFor) throws CLBuildException {
		if ((normalizeImage_kernel == null)) 
			normalizeImage_kernel = createKernel("normalizeImage");
		normalizeImage_kernel.setArgs(input, maxValue, output);
		return normalizeImage_kernel.enqueueNDRange(commandQueue, globalWorkSizes, localWorkSizes, eventsToWaitFor);
	}
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_NW = (int)1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_N = (int)2;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_NE = (int)1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_W = (int)0;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_C = (int)0;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_E = (int)0;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_SW = (int)-1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_S = (int)-2;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MX_SE = (int)-1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_NW = (int)1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_N = (int)0;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_NE = (int)-1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_W = (int)2;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_C = (int)0;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_E = (int)-2;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_SW = (int)1;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_S = (int)0;
	/** <i>native declaration : com/nativelibs4java/opencl/demos/sobelfilter/SimpleSobel.cl</i> */
	public static final int MY_SE = (int)-1;
}
