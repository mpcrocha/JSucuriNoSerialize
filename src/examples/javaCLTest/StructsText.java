package examples.javaCLTest;

import com.nativelibs4java.opencl.*;
import org.bridj.Pointer;
import org.bridj.ann.Struct;

import static org.bridj.Pointer.pointerToFloats;


/**
 * This is about the simplest possible JavaCL program.<br>
 * It adds two vectors of floats in parallel.<br>
 * This program can be written more easily with the JavaCL BLAS Library :
 * <code>
 * LinearAlgebraUtils la = new LinearAlgebraUtils();
 * CLKernel kernel = new LinearAlgebraUtils().getKernel(LinearAlgebraUtils.Fun2.add, LinearAlgebraUtils.Primitive.Float);
 * </code>
 * @author ochafik
 */
public class StructsText {

	public static void main(String[] args) {
        try {


			Pointer<Float> a = pointerToFloats(0.5f,  2.5f,  3.5f,  4.5f );
            Pointer<Float> b = pointerToFloats(10, 20, 30, 40);

			//Pointer<StructVectorAdd> ps = Pointer.getPointer(structVectorAdd);

            Pointer<Float> sum = add(a, b);
            for (long i = 0, n = sum.getValidElements(); i < n; i++)
                System.out.println(sum.get(i));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
	}

	public static Pointer<Float> add(Pointer<Float> a, Pointer<Float> b) throws CLBuildException {
		int n = (int)a.getValidElements();
		
		CLContext context = JavaCL.createBestContext();
		CLQueue queue = context.createDefaultQueue();
		
		String source =

			"__kernel void addFloats(__global const float* a, __global const float* b, __global float* output)     " +
			"{                                                                                                     " +
			"   int i = get_global_id(0);                                                                         " +
			"   output[i] = a[i] + b[i];                                                                          " +
			"}                                                                                                     ";
		
		CLKernel kernel = context.createProgram(source).createKernel("addFloats");

		CLBuffer<Float> aBuf = context.createBuffer(CLMem.Usage.Input, a, true);
		StructVectorAdd structVectorAdd = new StructVectorAdd();
		structVectorAdd.vecA = new float[]{0.5f, 1.0f};
		structVectorAdd.vecB = new float[]{0.5f, 1.0f};

		//CLBuffer<StructVectorAdd> textBuf = context.createBuffer(CLMem.Usage.Input, structVectorAdd, true );
		CLBuffer<Float> bBuf = context.createBuffer(CLMem.Usage.Input, b, true);
		CLBuffer<Float> outBuf = context.createBuffer(CLMem.Usage.Output, Float.class, n);
		kernel.setArgs(aBuf, bBuf, outBuf);
		
		kernel.enqueueNDRange(queue, new int[]{n});
		queue.finish();
	
		return outBuf.read(queue);
	}
}

