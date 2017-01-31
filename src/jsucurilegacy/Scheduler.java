package jsucurilegacy; /**
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

    List<PipedInputStream> worker_conns;
    List<PipedOutputStream> conn;

    public Scheduler(DFGraph graph, Integer n_workers, boolean mpi_enabled)
    {
        this.mpi_enabled = mpi_enabled; //must always be false, for now at least...

        this.operq = new ArrayBlockingQueue<List<Oper>>(10);
        this.workers = new ArrayList<Worker>();
        this.graph = graph;
        this.tasks = new ArrayList<Task>();

        this.worker_conns = new ArrayList<PipedInputStream>();
        this.conn = new ArrayList<PipedOutputStream>();

        this.n_workers = n_workers;
        this.pending_tasks = new ArrayList<Integer>(n_workers);

        for(int i = 0 ; i < n_workers ; i++)
            pending_tasks.add(0);

        this.waiting = new ArrayList<Integer>(n_workers);

        for(int i = 0 ; i < n_workers ; i++)
        {
            PipedInputStream worker_conn = new PipedInputStream(2048); //defaut size 1024
            PipedOutputStream sched_conn = new PipedOutputStream();

            try{
                worker_conn.connect(sched_conn);
            }
            catch( IOException e)
            {
                System.out.println(e + ": could not connect worker and scheduler pipes");
                e.printStackTrace();
            }
            finally {
                worker_conns.add(worker_conn);
                conn.add(sched_conn);
            }
        }

        for(int i = 0 ; i < n_workers ; i++)
        {
            workers.add(new Worker(this.graph, this.operq, worker_conns.get(i), i)); //i = workerid
            //workers.add(new Worker(this.graph, this.operq, i)); //i = workerid
        }

    }

//    public Scheduler(DFGraph graph, Integer n_workers, boolean mpi_enabled)
//    {
//        tasks_array = new Task[n_workers];
//        //this.sm = new Hashtable();
//
//        this.mpi_enabled = mpi_enabled; //must always be false, for now at least...
//
//        //this.operq = new SynchronousQueue();
//        //this.operq = new PriorityBlockingQueue<List<jsucuri.Oper>>(10,this); //10 foi escolhido aleatoriamente
//        this.operq = new ArrayBlockingQueue<List<Oper>>(10);
//        this.workers = new ArrayList<Worker>();
//        this.graph = graph;
//        this.tasks = new ArrayList<Task>();
//
////        this.worker_conns = new ArrayList<PipedInputStream>();
////        this.conn = new ArrayList<PipedOutputStream>();
//
//        this.n_workers = n_workers;
//        this.pending_tasks = new ArrayList<Integer>(n_workers);
//
//        for(int i = 0 ; i < n_workers ; i++)
//            pending_tasks.add(0);
//
//        this.waiting = new ArrayList<Integer>(n_workers);
//
//        //for(int i = 0 ; i < this.pending_tasks.size() ; i++)
//        //    this.pending_tasks.add(i,0);
//
////        for(int i = 0 ; i < n_workers ; i++)
////        {
////            PipedInputStream worker_conn = new PipedInputStream(2048); //defaut size 1024
////            PipedOutputStream sched_conn = new PipedOutputStream();
////
////            try{
////                worker_conn.connect(sched_conn);
////            }
////            catch( IOException e)
////            {
////                System.out.println(e + ": could not connect worker and scheduler pipes");
////                e.printStackTrace();
////            }
////            finally {
////                worker_conns.add(worker_conn);
////                conn.add(sched_conn);
////            }
////        }
//
//        for(int i = 0 ; i < n_workers ; i++)
//        {
//            //workers.add(new Worker(this.graph, this.operq, worker_conns.get(i), i)); //i = workerid
//            workers.add(new Worker(this.graph, this.operq, i)); //i = workerid
//        }
//
//    }

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

/*
    public Integer check_match(jsucuri.Node node)
    {
        if(node.inport != null)
        {
            for(int i = 0 ; i < node.inport.length ; i++)
            {
                System.out.println("jsucuri.Node[" + i + "]: " + node.inport[i]);
            }
        }

        for(int i = 0 ; i < node.inport[0].size() ; i++)
        {
            jsucuri.TagVal tv = node.inport[0].get(i);
            int count = 1;

            for(int j = 1 ; j < node.inport.length ; j++)
            {
                List<jsucuri.TagVal> port = node.inport[j];

                if(port.contains(tv))
                    count++;
            }

            if(count == node.inport.length)
                return tv.tag;
        }

        return null;
    }
*/

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
        for (int i= 0 ; i < this.worker_conns.size() ; i++) {
            PipedInputStream pi = this.worker_conns.get(i);
            try {
                pi.close();
            }catch(IOException e)
            {
                System.out.println("Cannot close PipedInputStream. " + e);
                e.printStackTrace();
            }
        }

        for (int i= 0 ; i < this.conn.size() ; i++) {
            PipedOutputStream po = this.conn.get(i);
            try {
                po.close();
            }catch(IOException e)
            {
                System.out.println("Cannot close PipedOutputStream. " + e);
                e.printStackTrace();
            }
        }

        System.out.println("Terminating workers " + all_idle() + " " + operq.size() + " " + tasks.size());
        for (Worker w : workers)
        {
            PipedInputStream pi = w.getPipeInput();
            try{
                pi.close();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }

            w.terminate();

            /*jsucuri.Worker w = this.workers.get(i);
            try{
                w.join();
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }*/
        }
    }

//    public void terminate_workers()
//    {
////        for (int i= 0 ; i < this.worker_conns.size() ; i++) {
////            PipedInputStream pi = this.worker_conns.get(i);
////            try {
////                pi.close();
////            }catch(IOException e)
////            {
////                System.out.println("Cannot close PipedInputStream. " + e);
////                e.printStackTrace();
////            }
////        }
////
////        for (int i= 0 ; i < this.conn.size() ; i++) {
////            PipedOutputStream po = this.conn.get(i);
////            try {
////                po.close();
////            }catch(IOException e)
////            {
////                System.out.println("Cannot close PipedOutputStream. " + e);
////                e.printStackTrace();
////            }
////        }
//
//        System.out.println("Terminating workers " + all_idle() + " " + operq.size() + " " + tasks.size());
//        for (Worker w : workers)
//        {
////            PipedInputStream pi = w.getPipeInput();
////            try{
////                pi.close();
////            }
////            catch(IOException e)
////            {
////                e.printStackTrace();
////            }
//
//            w.terminate();
//
//            /*jsucuri.Worker w = this.workers.get(i);
//            try{
//                w.join();
//            }
//            catch(InterruptedException e)
//            {
//                e.printStackTrace();
//            }*/
//        }
//    }

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

                    ObjectOutputStream oos = null;
                    try{
                        oos = new ObjectOutputStream(new BufferedOutputStream(conn.get(worker.wid)));
                        //System.out.println("Sent: " + task);
                        oos.writeObject(task);
                        oos.flush();
                        //oos.close();
                    }catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                }


            }


        }
        System.out.println("Waiting " + this.waiting.size());
        terminate_workers();
        System.out.println("jsucuri.Scheduler finished!");
    }

//    public void main_loop()
//    {
//        while(this.tasks.size() > 0 || !all_idle() || this.operq.size() > 0 )
//        {
//            List<Oper> opermsg = null;
//            try {
//                opermsg = operq.take();
//            }
//            catch(InterruptedException e)
//            {
//                e.printStackTrace();
//            }
//            finally {
//                if(opermsg == null)
//                {
//                    System.out.println("Error! Opermsg null!");
//                    System.exit(0);
//                }
//            }
//
//            for (int i = 0; i < opermsg.size(); i++) {
//                Oper oper = opermsg.get(i);
//                if (oper.value != null) {
//                    propagate_op(oper);
//                }
//            }
//
//            Integer wid = opermsg.get(0).wid;
//
//            if(!waiting.contains(wid) && opermsg.get(0).request_task)
//            {
//                if(pending_tasks.get(wid) > 0)
//                    pending_tasks.set(wid,pending_tasks.get(wid) - 1);
//                else
//                    waiting.add(wid);
//            }
//
//            while(tasks.size() > 0 && waiting.size() > 0)
//            {
//                Task task = tasks.remove(0);
//                wid = check_affinity(task);
//                if(wid != null)
//                {
//                    if(waiting.contains(wid))
//                        waiting.remove(wid);
//                    else
//                        pending_tasks.set(wid,pending_tasks.get(wid) + 1);
//                }
//                else
//                {
//                    wid = waiting.remove(0);
//                }
//
//                if(wid < n_workers)
//                {
//                    Worker worker = workers.get(wid);
//
//                    if(worker.activeTask == null) {
//                        worker.activeTask = task;
//                    }
//                    else {
//                        while (worker.activeTask != null) {
//                            //do nothing
//                        }
//                        worker.activeTask = task;
//                    }
//
//                    //System.out.println("scheduler active task: " + worker.activeTask);
//
////                    ObjectOutputStream oos = null;
////                    try{
////                        oos = new ObjectOutputStream(new BufferedOutputStream(conn.get(worker.wid)));
////                        //System.out.println("Sent: " + task);
////                        oos.writeObject(task);
////                        oos.flush();
////                        //oos.close();
////                    }catch (IOException e)
////                    {
////                        e.printStackTrace();
////                    }
//                }
//
//
//            }
//
//
//        }
//        System.out.println("Waiting " + this.waiting.size());
//        terminate_workers();
//        System.out.println("jsucuri.Scheduler finished!");
//    }

    /*
    public int compare(Object o1, Object o2)
    {
        jsucuri.Oper op1 = (jsucuri.Oper) o1;
        jsucuri.Oper op2 = (jsucuri.Oper) o2;

        if(op1.tag > op2.tag)
        {
            return 1;
        }
        else if(op1.tag < op2.tag)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }
*/

}

