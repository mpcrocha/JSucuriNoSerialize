package test;

import jsucuriinoserialize.*;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

/**
 * Created by marcos on 15/03/17.
 */
public class SourceNodeTest2 {

        public static void main(String[] args) {

            NodeFunction printResult = new NodeFunction() {
                @Override
                public Object f(Object[] in_operands) {
                    System.out.println("Result: " + in_operands[0]);
                    return in_operands[0];
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
            Node n1 = new Node(printResult, 1);
            Node n2 = new Node(printResult, 1);
            Node n3 = new Node(printResult, 1);
            Node n4 = new Node(printResult, 2);

            dfg.add(sourceNode);
            dfg.add(n1);
            dfg.add(n2);
            dfg.add(n3);
            dfg.add(n4);

            sourceNode.add_edge(n1, 0);
            n1.add_edge(n2, 0);
            n2.add_edge(n3, 0);
            n3.add_edge(n4, 0);
            sourceNode.add_edge(n4, 1);

            Scheduler sched = new Scheduler(dfg,3,false);
            sched.start();

        }
}
