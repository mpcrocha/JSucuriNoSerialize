package jsucuriinoserialize; /**
 * Created by alexandrenery on 9/20/16.
 */

import java.io.BufferedInputStream;
import java.io.ObjectInputStream;
import java.io.PipedInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;

//Workers receive Tasks from jsucuri.Scheduler and Executes them
public class Worker extends Thread
{
    public DFGraph graph;
    public ArrayBlockingQueue operq;
    public Integer wid;
    public boolean idle;
    private boolean terminate;
    public Task activeTask;
    public ConcurrentLinkedQueue<Task> taskQueue;

    public Worker(DFGraph graph, ArrayBlockingQueue operand_queue, int workerid) {
        this.activeTask = null;
        this.terminate = false;
        this.operq = operand_queue;
        this.idle = false;
        this.graph = graph;
        this.wid = workerid;
        this.taskQueue = new ConcurrentLinkedQueue<Task>();
    }

    public void terminate()
    {
        this.terminate = true;
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

            if(!taskQueue.isEmpty()){
                task = taskQueue.poll();
                System.out.println("task.nf = " + task.nf);

                node = this.graph.nodes.get(task.nodeid);
                node.run(task.args, this.wid, this.operq);
            }

        }
        System.out.println("jsucuri.Worker finished!");
    }

}


