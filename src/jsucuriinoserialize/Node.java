package jsucuriinoserialize; /**
 * Created by alexandrenery on 9/20/16.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;


public class Node
{
    public NodeFunction nf;
    public List<TagVal> inport[];
    public List<Edge> dsts;
    public Integer id;
    public Integer affinity;
    public Integer inputn;


    public Node()
    {
        this.nf = null;
        this.inputn = 0;
        this.inport = new ArrayList[0];
        this.dsts = new ArrayList<Edge>();
        this.affinity = null;

    }

    public Node(NodeFunction nf, Integer inputn)
    {
        this.nf = nf;
        this.inputn = inputn;

        this.inport = new ArrayList[inputn];
        for(int i = 0 ; i < this.inport.length ; i++)
            inport[i] = new ArrayList();

        this.dsts = new ArrayList();
        this.affinity = null;

        /*
        if(inputn > 0){
            this.inport = new ArrayList[inputn];

            for (int i = 0 ; i < this.inport.length ; i++)
                inport[i] = new ArrayList();
        }
        else
        {
            this.inport = null;
        }

        this.dsts = new ArrayList();
        this.affinity = null;
        */
    }

    public void add_edge(Node dst, Integer dstport)
    {
        this.dsts.add(new Edge(dst.id, dstport));
    }

    public void pin(Integer workerid)
    {
        this.affinity = workerid;
    }

    //public void run(Object[] args, Integer workerid, SynchronousQueue operq)
    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq)
    {
        if(inport.length == 0)
        {
            System.out.println("jsucuri.Worker " + workerid + " running node " + id + " with (null args)");
            Object value = this.nf.f(null);
            List opers = create_oper(value, workerid, operq,0); //default tag = 0
            sendops(opers,operq);
        }
        else
        {
            Object value = this.nf.f(args);
            List opers = create_oper(value, workerid, operq,0); //default tag = 0
            sendops(opers,operq);
        }
    }

    //public void sendops(List opers, SynchronousQueue operq)
    public void sendops(List opers, ArrayBlockingQueue operq)
    {
        try {

            //Iterator it = operq.iterator();
            //while(it.hasNext())
            //{
            //    System.out.println("\toper: " + it.next());
            //}

            operq.put(opers);
        }catch(InterruptedException e)
        {
            System.out.println("operq.put error: " + e);
        }
    }

    //public List create_oper(Object value, Integer workerid, SynchronousQueue operq, Integer tag)
    public List create_oper(Object value, Integer workerid, ArrayBlockingQueue operq, Integer tag)
    {
        List opers = new ArrayList();

        if(this.getDsts().size() == 0)
        {
            opers.add(new Oper(workerid,null,null,null));
        }
        else
        {
            for(Edge edge: dsts){
                Oper oper = new Oper(workerid, edge.dst_id, edge.dst_port, value);
                oper.tag = tag;
                opers.add(oper);
            }

        }
        return opers;
    }


    public List<TagVal>[] getInport(){
         return inport;
     }
    public List getDsts(){return dsts;}
}
