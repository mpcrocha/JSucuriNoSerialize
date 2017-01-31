package jsucuriinoserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Iterator;

/**
 * Created by marcos on 08/10/16.
 */
public class N2D extends Node {

    public LCSNodeFunction nf;
    public Integer i;
    public Integer j;

    public N2D()
    {
        super();
    }

    public N2D(LCSNodeFunction nf, Integer inputn, Integer i, Integer j){
        this.nf = nf;
        this.inputn = inputn;
        this.i = i;
        this.j = j;

        this.inport = new ArrayList[inputn];
        for(int k = 0 ; k < this.inport.length ; k++)
            inport[k] = new ArrayList<TagVal>();

        this.dsts = new ArrayList<Edge>();
        this.affinity = null;

        //this.inport = new ArrayList[inputn];
        /*if(inputn > 0){
            this.inport = new ArrayList[inputn];

            for (int k = 0 ; k < this.inport.length ; k++)
                inport[k] = new ArrayList();
        }
        else
        {
            this.inport = null;
        }

        this.dsts = new ArrayList();
        this.affinity = null;
        */
    }

    public void add_edge(Node dst, Integer dstport, Integer srcport)
    {
        this.dsts.add(new Edge(dst.id, dstport, srcport));
    }

    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq){
        Object output[];

        /*
        System.out.println("inport.length = " + inport.length);
        if(args != null) {
            for (int i = 0; i < args.length; i++) {
                System.out.println("args[" + i + "]:" + args[i]);
            }
        }
        */

        if(inport.length == 0) {
            //if(inport.length == 0) {
            //args = new Object[2];
            //args[args.length-2] = i;
            //args[args.length-1] = j;
            output = nf.f(null,this.i,this.j);
            //output = nf.f(args);
        }else{
            //args = new Object[args.length+2];
            //args[args.length-1] = i;
            //args[args.length] = j;
            output = nf.f(args,this.i,this.j);
        }
        List opers = create_oper(output, workerid, operq, 0);
        sendops(opers,operq);
    }

    //public List create_oper(Object value, Integer workerid, SynchronousQueue operq, Integer tag)
    public List create_oper(Object[] value, Integer workerid, ArrayBlockingQueue operq, Integer tag)
    {
        List opers = new ArrayList();

        if(this.getDsts().size() == 0)
        {
            opers.add(new Oper(workerid,null,null,null));
        }
        else
        {
            Iterator it = dsts.iterator();
            while(it.hasNext())
            {
                Edge e = (Edge) it.next();
                Oper oper = new Oper(workerid, e.dst_id, e.dst_port, value[e.srcport]);
                oper.tag = tag;
                opers.add(oper);
            }

            //dsts.forEach((e) -> {
            //    Oper oper = new Oper(workerid, e.dst_id, e.dst_port, value[e.srcport]);
            //    oper.tag = tag;
            //    opers.add(oper);
            //});
        }
        return opers;
    }


    /*public List create_oper(Object value, Integer workerid, PriorityBlockingQueue operq, Integer tag)
    {
        List opers = new ArrayList();

        if(this.getDsts().size() == 0)
        {
            opers.add(new Oper(workerid, null ,null ,null));
        }
        else
        {
            dsts.forEach((e) -> {
                Edge edge = (Edge) e;
                System.out.println("workerid:" + workerid);
                System.out.println("dstid:" + edge.dst_id );
                System.out.println("dstport:"+ edge.dst_port);
                System.out.println("value[srcport]:" + ((Object[])value)[edge.srcport]);
                Oper oper = new Oper(workerid, edge.dst_id, edge.dst_port, ((Object[])value)[edge.srcport]);
                oper.tag = tag;
                opers.add(oper);
            });
        }
        return opers;
    }*/

}
