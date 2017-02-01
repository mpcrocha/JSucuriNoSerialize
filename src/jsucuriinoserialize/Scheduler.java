package jsucuriinoserialize; /**
 * Created by alexandrenery on 9/20/16.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

//class jsucuri.Scheduler implements Comparator
public class Scheduler
{
    ArrayBlockingQueue<List<Oper>> operq;
    DFGraph graph;
    List<Task> tasks;
    List<Integer> waiting; //list of idle workers
    Integer n_workers;
    List<Integer> pending_tasks;
    List<Worker> workers;
    boolean mpi_enabled;

    //List<PipedInputStream> worker_conns;
    //List<PipedOutputStream> conn;

    public Scheduler(DFGraph graph, Integer n_workers, boolean mpi_enabled)
    {
        this.mpi_enabled = mpi_enabled; //must always be false, for now at least...

        this.operq = new ArrayBlockingQueue<List<Oper>>(10);
        this.workers = new ArrayList<Worker>();
        this.graph = graph;
        this.tasks = new ArrayList<Task>();

        //this.worker_conns = new ArrayList<PipedInputStream>();
        //this.conn = new ArrayList<PipedOutputStream>();

        this.n_workers = n_workers;
        this.pending_tasks = new ArrayList<Integer>(n_workers);

        for(int i = 0 ; i < n_workers ; i++)
            pending_tasks.add(0);

        this.waiting = new ArrayList<Integer>(n_workers);

        for(int i = 0 ; i < n_workers ; i++)
        {
            workers.add(new Worker(this.graph, this.operq, i)); //i = workerid
            //workers.add(new Worker(this.graph, this.operq, i)); //i = workerid
        }

    }

    public Integer check_match(Node node)
    {
        for(int i = 0 ; i < node.getInport()[0].size() ; i++)
        {
            TagVal tv = node.getInport()[0].get(i);
            //System.out.println(tv);
            int count = 1;

            for(int j = 1 ; j < node.getInport().length ; j++)//j = i + 1 (errado!)
            {
                List<TagVal> port = node.getInport()[j];
                //System.out.print(j + ":");
                //for(int k = 0 ; k < port.size() ; k++)
                //    System.out.print("" + port.get(k));

                //System.out.println();

                for(int k = 0 ; k < port.size() ; k++)
                {
                    TagVal tv2 = port.get(k);

                    if(tv.tag == tv2.tag)
                    {
                        count++;
                        break;
                    }
                }

            }

            //System.out.println("count = " + count);
            //System.out.println("inport.length = " + node.inport.length);


            if(count == node.getInport().length)
                return tv.tag;
        }

        return null;

    }


    public void propagate_op(Oper oper)
    {
        Node dst = this.graph.nodes.get(oper.dstid);

        //System.out.println("dst = " + dst);
        //System.out.println("dst.inport = " + dst.inport);
        //System.out.println("oper.dstport = " + oper.dstport);

        dst.inport[oper.dstport].add(new TagVal(oper.tag, oper.value));

        Integer tag = check_match(dst);

        if(tag != null)
            issue(dst,tag);
    }

    public Integer check_affinity(Task task)
    {
        Node node = this.graph.nodes.get(task.nodeid);
        return node.affinity;
    }

    public void issue(Node node, Integer tag)
    {
        List<Object> args = new ArrayList<Object>();

        for(int i = 0 ; i < node.getInport().length ; i++)
        {
            List<TagVal> port = node.getInport()[i];

            List<TagVal> laux = new ArrayList<TagVal>();
            for(int j = 0 ; j < port.size() ; j++)
            {
                TagVal tagval = port.get(j);
                if(tagval.tag == tag)
                    laux.add(tagval);
            }

            TagVal tv = laux.get(0);
            args.add(tv.val);
            port.remove(tv);
        }

        //args.add(sm);

        Task t = new Task(node.nf, node.id, args.toArray());
        tasks.add(t);
    }

    public boolean all_idle()
    {
        return (this.waiting.size() == this.n_workers);
    }

    public void terminate_workers()
    {
        System.out.println("Terminating workers " + all_idle() + " " + operq.size() + " " + tasks.size());
        for (Worker w : workers)
        {
            w.terminate();
        }
    }

    public void start()
    {
        for(int i = 0 ; i < this.graph.nodes.size() ; i ++)
        {
            Node n = this.graph.nodes.get(i);
            if(n.inport.length == 0) //if root node (no input ports)
            {
                Task t = new Task(n.nf,n.id,null);
                this.tasks.add(t);
            }
        }

        for(int i = 0 ; i < this.workers.size() ; i++)
        {
            Worker w = this.workers.get(i);
            System.out.println("Starting worker: " + w);
            w.start();
        }

        main_loop();
    }

    public void main_loop()
    {
        long totalWriteTime = 0l;
        while(this.tasks.size() > 0 || !all_idle() || this.operq.size() > 0 )
        {
            List<Oper> opermsg = null;
            try {
                opermsg = operq.take();
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            finally {
                if(opermsg == null)
                {
                    System.out.println("Error! Opermsg null!");
                    System.exit(0);
                }
            }

            for (int i = 0; i < opermsg.size(); i++) {
                Oper oper = opermsg.get(i);
                if (oper.value != null) {
                    propagate_op(oper);
                }
            }

            Integer wid = opermsg.get(0).wid;

            if(!waiting.contains(wid) && opermsg.get(0).request_task)
            {
                if(pending_tasks.get(wid) > 0)
                    pending_tasks.set(wid,pending_tasks.get(wid) - 1);
                else
                    waiting.add(wid);
            }

            while(tasks.size() > 0 && waiting.size() > 0)
            {
                Task task = tasks.remove(0);
                wid = check_affinity(task);
                if(wid != null)
                {
                    if(waiting.contains(wid))
                        waiting.remove(wid);
                    else
                        pending_tasks.set(wid,pending_tasks.get(wid) + 1);
                }
                else
                {
                    wid = waiting.remove(0);
                }

                if(wid < n_workers)
                {
                    Worker worker = workers.get(wid);
                    worker.taskQueue.add(task);
                }
            }
        }
        System.out.println("Waiting " + this.waiting.size());
        terminate_workers();
        System.out.println("TotalWriteTime: "+ totalWriteTime);
        System.out.println("jsucuri.Scheduler finished!");
    }

}

