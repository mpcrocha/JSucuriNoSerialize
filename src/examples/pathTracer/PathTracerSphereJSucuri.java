package examples.pathTracer;

import jsucuriinoserialize.DFGraph;
import jsucuriinoserialize.Scheduler;

/**
 * Created by marcos on 06/05/17.
 */
public class PathTracerSphereJSucuri {
    public static void main(String args[]){

        int numWorkers = new Integer(args[0]);

        DFGraph dfg = new DFGraph();


        /*Add some Nodes
        dfg.add(A);
        dfg.add(B);
        dfg.add(C);
        dfg.add(D);
        */

        /*Add some edges
        A.add_edge(C, 0);
        B.add_edge(C, 1);
        C.add_edge(D, 0);
        */

        //start scheduler
        Scheduler sched = new Scheduler(dfg, numWorkers, false);
        sched.start();
    }
}
