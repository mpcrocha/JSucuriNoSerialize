package jsucurilegacy; /**
 * Created by alexandrenery on 9/20/16.
 */

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

//Workers receive Tasks from jsucuri.Scheduler and Executes them
public class Worker extends Thread
{
    public DFGraph graph;
    public ArrayBlockingQueue operq;
    public PipedInputStream conn; //piped input stream
    public Integer wid;
    public boolean idle;
    private boolean terminate;
    public Task activeTask;

    //public jsucuri.Worker(jsucuri.DFGraph graph, SynchronousQueue operand_queue, PipedInputStream conn, int workerid)
    public Worker(DFGraph graph, ArrayBlockingQueue operand_queue, PipedInputStream conn, int workerid)
    {
        this.activeTask = null;
        this.terminate = false;
        this.operq = operand_queue;
        this.idle = false;
        this.graph = graph;
        this.wid = workerid;
        this.conn = conn; //it MUST already be connected to the other end
    }

    public Worker(DFGraph graph, ArrayBlockingQueue operand_queue, int workerid) {
        this.activeTask = null;
        this.terminate = false;
        this.operq = operand_queue;
        this.idle = false;
        this.graph = graph;
        this.wid = workerid;
        this.conn = null;
    }

    public void terminate()
    {
        this.terminate = true;
    }

    public PipedInputStream getPipeInput()
    {
        return this.conn;
    }

    @Override
    public void run()
    {
        System.out.println("I am worker " + wid);
        List l = new ArrayList();
        l.add(new Oper(this.wid, null, null, null));

        try {
            this.operq.put(l); //request a task to start
        }catch(InterruptedException e)
        {
            System.out.println("operq.put error:" + e);
        }

        while(!terminate)
        {

            Task task;
            Node node;
            ObjectInputStream ois = null;

            try{
                if(this.conn.available() > 0) {
                    ois = new ObjectInputStream(new BufferedInputStream(this.conn));
                    task = (Task) ois.readObject();
                    System.out.println("task.nf = " + task.nf);

                    //ois.close();
                    //System.out.println("Recv: " + task);
                    node = this.graph.nodes.get(task.nodeid);

                    node.run(task.args, this.wid, this.operq);
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }

        }
        System.out.println("jsucuri.Worker finished!");
    }

//    @Override
//    public void run()
//    {
//        System.out.println("I am worker " + wid);
//        List l = new ArrayList();
//        l.add(new Oper(this.wid, null, null, null));
//
//        try {
//            this.operq.put(l); //request a task to start
//        }catch(InterruptedException e)
//        {
//            System.out.println("operq.put error:" + e);
//        }
//
//        //while(true)
//        while(!terminate)
//        {
//
////            Task task;
////            Node node;
////            ObjectInputStream ois = null;
////
////            try{
////                if(this.conn.available() > 0) {
////                    ois = new ObjectInputStream(new BufferedInputStream(this.conn));
////                    task = (Task) ois.readObject();
////                    //ois.close();
////                    //System.out.println("Recv: " + task);
////                    node = this.graph.nodes.get(task.nodeid);
////
////                    node.run(task.args, this.wid, this.operq);
////                }
////            }
////            catch(Exception e)
////            {
////                e.printStackTrace();
////            }
//            //System.out.println("worker active task: " + this.activeTask);
//            Node node;
//            if(this.activeTask != null)
//            {
//                //System.out.println("Entrou no if");
//                node = this.graph.nodes.get(this.activeTask.nodeid);
//                node.run(this.activeTask.args, this.wid, this.operq);
//                this.activeTask = null;
//            }
//
//        }
//        System.out.println("jsucuri.Worker finished!");
//    }
}


