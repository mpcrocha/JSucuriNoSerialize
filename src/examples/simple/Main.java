package examples.simple;

import jsucurilegacy.*;

import java.io.Serializable;

public class Main {

    public static void main(String[] args) {
        // write your code here
        DFGraph dfg = new DFGraph();

        NodeFunction soma = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                Integer result;

                result = ((Integer) in_operands[0] + (Integer) in_operands[1]);

                return result;

            }
        };

//        NodeFunction soma = (NodeFunction & Serializable) (Object[] inputs) -> {
//            Integer result;
//            result = new Integer(0);
//
//
//            //result = ((Integer) inputs[0] + (Integer) inputs[1]);
//
//
//            return result;
//            };


        NodeFunction printResult = new NodeFunction() {
            @Override
            public Object f(Object[] in_operands) {
                System.out.println("Result: " + in_operands[0]);
                return null;
            }
        };

//        NodeFunction printResult = (NodeFunction & Serializable) (Object[] inputs) -> {
//
//            System.out.println("Result: " + inputs[0]);
//            return null;
//        };

        System.out.println("Testando...");

        Feeder A = new Feeder(new Integer(1));
        Feeder B = new Feeder(new Integer(2));
        Node C = new Node(soma,2);
        Node D = new Node(printResult,1);

        dfg.add(A);
        dfg.add(B);
        dfg.add(C);
        dfg.add(D);

        A.add_edge(C, 0);
        B.add_edge(C, 1);
        C.add_edge(D, 0);

        Scheduler sched = new Scheduler(dfg,3,false);
        sched.start();

    }
}
