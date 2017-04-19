package examples.vrc;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.Serializable;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class Util implements Serializable{

    public static byte[] loadRawFileBytes(String filename, int bytes) throws IOException
    {
        File file = new File(filename);

        if(!file.exists())
            return null;

        byte array[] = new byte[(int) file.length()];
        RandomAccessFile ra = new RandomAccessFile(filename,"r");

        ra.read(array);

        return array;
    }

    public static float[] loadRawFileFloats(String filename, int bytes) throws IOException
    {
        File file = new File(filename);

        if(!file.exists())
            return null;

        //byte array[] = new byte[(int) file.length()];
        byte array[] = new byte[(int) bytes];
        RandomAccessFile ra = new RandomAccessFile(filename,"r");

        ra.read(array);
        System.out.println("file.length: " + file.length());

        float[] result = new float[array.length];

        for(int i = 0 ; i < array.length ; i++)
        {
            result[i] = (float) ((float) array[i]/ (float) Byte.MAX_VALUE);
        }
        return result;
    }

}
