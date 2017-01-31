package examples.lcs;

import jsucurilegacy.*;

import java.io.*;
import java.io.Serializable;

/**
 * Created by marcos on 08/10/16.
 */
public class LCS {

    static int block;
    static int gW;
    static int gH;
    static int sizeA;
    static int sizeB;
    static String sA;
    static String sB;

    public static void main(String[] args) {

        int nprocs = 10;
        DFGraph lcsGraph = new DFGraph();
        Scheduler sched = new Scheduler(lcsGraph, nprocs, false);

        String nameA = "/home/anery/jsucurilegacy/src/examples/lcs/seqA.txt";//sys.argv[1]
        String nameB = "/home/anery/jsucurilegacy/src/examples/lcs/seqB.txt";//sys.argv[2]

        try {
            sA = readFile(nameA);
            sB = readFile(nameB);
            //sA = new String(Files.readAllBytes(Paths.get(nameA)));
            //sB = new String(Files.readAllBytes(Paths.get(nameB)));
        } catch (IOException e) {
            e.printStackTrace();
        }

        sizeA = sA.length() - (sA.charAt(sA.length()-1) == '\n' ? 1 : 0);
        sizeB = sB.length() - (sB.charAt(sB.length()-1) == '\n' ? 1 : 0);

        System.out.println("Sizes " + sizeB + " x " + sizeA);
        block = 1;

        gH = (int)(Math.ceil((float)(sizeB)/block));
        gW = (int)(Math.ceil((float)(sizeA)/block));

        System.out.println("Grid " + gH + " x " + gW);

        //compute lambda function
        LCSNodeFunction compute = new LCSNodeFunction() {
            @Override
            public Object[] f(Object[] in_operands, int i, int j) {
                return LCS(in_operands,i,j);
            }
        };

//        //compute lambda function
//        LCSNodeFunction compute = (LCSNodeFunction & Serializable) (Object[] oper, int i, int j) -> {
//            return LCS(oper,i,j);
//        };


        //building the dataflow graph
        N2D[][]G = new N2D[gH][gW];
        for(int i=0; i< gH;i++){
            for(int j=0; j< gW; j++ ){
                G[i][j] = new N2D(compute, inputs(i, j), i, j);
            }

        }

        for(int i=0; i < gH;i++) {
            for (int j = 0; j < gW; j++) {
                lcsGraph.add(G[i][j]);
            }
        }

        for(int i=0; i< gH;i++) {
            for (int j = 0; j < gW; j++) {
                if(i > 0){
                    G[i-1][j].add_edge(G[i][j], 0, 0);
                }if(j > 0){
                    G[i][j-1].add_edge(G[i][j], (i > 0 ? 1 : 0) ,1);
                }
            }
        }

        //print lambda function
        NodeFunction printLCS = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                Integer[] op = (Integer[]) in_operands[0];

                System.out.println("Score: " + op[op.length-1]);
                return null;
            }
        };

//        //print lambda function
//        NodeFunction printLCS = (NodeFunction & Serializable) (Object[] oper) -> {
//            Integer[] op = (Integer[]) oper[0];
//
//            System.out.println("Score: " + op[op.length-1]);
//            return null;
//        };


        Node R = new Node(printLCS, 1);
        lcsGraph.add(R);
        G[gH-1][gW-1].add_edge(R, 0);

        //System.out.println("Graph: " + lcsGraph.toString());

        sched.start();


    }

    static String readFile(String file) throws IOException{
        BufferedReader reader = new BufferedReader(new FileReader(file));
        String line = null;
        StringBuilder sb = new StringBuilder();
        String ls = System.getProperty("line.separator");

        try{
            while((line = reader.readLine()) != null){
                sb.append(line);
                sb.append(ls);
            }
            return sb.toString();
        }finally{
            reader.close();
        }
    }

    static int inputs(int i, int j){
        if(i==0 && j==0)
            return 0;
        if(i==0 || j ==0)
            return 1;
        return 2;
    }


    static Object[] LCS(Object[] oper,int i, int j){
        int startA = j*block;
        int endA = 0;
        if ((j+1) == gW)
            endA = sizeA;
        else
            endA = startA+block;

        int startB = i*block;
        int endB = 0;
        if ((i+1) == gH)
            endB = sizeB;
        else
            endB = startB+block;
        int lsizeA = endA - startA;
        int lsizeB = endB - startB;

        //#print 'Node (%d,%d) calculates (%d,%d) - (%d,%d)' % (i,j, startB,startA,endB,endA)
        System.out.println("lsizeA = " + lsizeA + ", lsizeB = " + lsizeB);
        System.out.println("Node (" + i + "," + j + ") calculates (" + startB + "," + startA + ") - (" + endB + "," + endA + ")");

        //int SM[][] = new int[lsizeA+1][lsizeB+1];
        int SM[][] = new int[lsizeB+1][lsizeA+1];
        for(int x = 0; x < lsizeB+1; x++){
            for(int y = 0; y < lsizeA+1; y++) {
                SM[x][y] = 0;
                System.out.print(" " + SM[x][y] + " ");
            }
            System.out.println();
        }

        int port = 0;
        if(i>0) {
            Integer[] op = (Integer[]) oper[0];
            System.out.println("oper[0].length = " + op.length);
            for (int x = 0; x < op.length; x++) {
                SM[0][x] = op[x];

            }
            port = port + 1;
        }
        if(j>0) {
            Integer[] op = (Integer[]) oper[port];
            System.out.println("oper[port].length = " + op.length);
            for(int x=0; x < op.length; x++) {
                SM[x][0] = op[x];
            }
        }

        for(int ii = 1; ii <lsizeB+1; ii++){
            for(int jj = 1; jj < lsizeA+1; jj++) {
                if (sB.charAt(startB + ii - 1) == sA.charAt(startA + jj - 1))
                    SM[ii][jj] = SM[ii - 1][jj - 1] + 1;
                else
                    SM[ii][jj] = Math.max(SM[ii][jj - 1], SM[ii - 1][jj]);
            }

        }

        Integer s1[] = new Integer[lsizeB+1];
        Integer s2[] = new Integer[lsizeA+1];

        Integer[][] ret = new Integer[2][];

        for(int q = 0 ; q < SM.length ; q++)
        {
            s1[q] = SM[q][lsizeA];
        }

        for(int q = 0 ; q < SM[0].length ; q++)
        {
            s2[q] = SM[lsizeB][q];
        }

        ret[0] = s2;
        ret[1] = s1;

        return ret;
    }


}
