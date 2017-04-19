package test;

import jsucuriinoserialize.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by marcos on 15/03/17.
 */
public class SourceNodeTest {

        public static void main(String[] args) {

            NodeFunction printResult = new NodeFunction() {
                @Override
                public Object f(Object[] in_operands) {
                    System.out.println("Result: " + in_operands[0]);
                    return null;
                }
            };


            DFGraph dfg = new DFGraph();

            System.out.println("Testando...");
            BufferedReader bufferedReader = null;
            try {
                bufferedReader = new BufferedReader(new FileReader("TextSource.txt"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            Source sourceNode = new Source(bufferedReader);
            Node D = new Node(printResult, 1);

            dfg.add(sourceNode);
            dfg.add(D);

            sourceNode.add_edge(D, 0);

            Scheduler sched = new Scheduler(dfg,3,false);
            sched.start();

        }
}
