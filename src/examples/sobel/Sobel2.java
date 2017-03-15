package examples.sobel;

/*
 * https://www.cyut.edu.tw/~yltang/program/Sobel.java
 **************************************************************************
 *
 *   Sobel2 operator
 *
 **************************************************************************
 */
import java.io.*;

public class Sobel2 {

    public static void main(String[] args) {
        int     i, j, nrows, ncols, img[][];
        double  Gx[][], Gy[][], G[][];

        if (args.length != 6) {
            System.out.println(
                    "Usage: Sobel2 <nrows> <ncols> <in_img> <Gx> <Gy> <G>");
            System.exit(0);
        }
        //System.out.println("Tracing...");
        //long time1 = System.currentTimeMillis();

        nrows = Integer.parseInt(args[0]);
        ncols = Integer.parseInt(args[1]);
        img = new int[nrows][ncols];
        ArrayIO.readByteArray(args[2], img, nrows, ncols);
        Gx = new double[nrows][ncols];
        Gy = new double[nrows][ncols];
        G  = new double[nrows][ncols];


        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        for (i=0; i<nrows; i++) {
            for (j=0; j<ncols; j++) {
                if (i==0 || i==nrows-1 || j==0 || j==ncols-1)
                    Gx[i][j] = Gy[i][j] = G[i][j] = 0; // Image boundary cleared
                else{
                    Gx[i][j] = img[i+1][j-1] + 2*img[i+1][j] + img[i+1][j+1] -
                            img[i-1][j-1] - 2*img[i-1][j] - img[i-1][j+1];
                    Gy[i][j] = img[i-1][j+1] + 2*img[i][j+1] + img[i+1][j+1] -
                            img[i-1][j-1] - 2*img[i][j-1] - img[i+1][j-1];
                    G[i][j]  = Math.abs(Gx[i][j]) + Math.abs(Gy[i][j]);
                }
            }
        }

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");

        //ArrayIO.writeDoubleArray(args[3], Gx, nrows, ncols);
        //ArrayIO.writeDoubleArray(args[4], Gy, nrows, ncols);
        ArrayIO.writeDoubleArray(args[5], G, nrows, ncols);
    }

}


