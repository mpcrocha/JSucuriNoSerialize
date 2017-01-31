package jsucuriinoserialize;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.Iterator;

/**
 * Created by marcos on 01/10/16.
 */

public class Source extends Node {
    List<TagVal> inport[];
    List dsts;
    Integer tagcounter = 0;
    BufferedReader it;


    public Source(){

    }

    public Source(BufferedReader it){
        this.it = it;
        this.dsts = new ArrayList();
    }

    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq){
        try {
            String line;
            while ((line = it.readLine()) != null) {
                String result = f(line, null);

                Integer tag = this.tagcounter;
                List opers = create_oper(new TaggedValue(result, tag), workerid, operq, tag);


                for (Object oper : opers) {
                    ((Oper)oper).request_task = false;

                }
                sendops(opers, operq);
                this.tagcounter += 1;
            }

        }catch (IOException ioEx){
            ioEx.printStackTrace();
        }


        List opers = new ArrayList();
        opers.add(new Oper(workerid, null, null, null)); //sinalize eof and request a task
        sendops(opers, operq);
    }

    String f(String line, Object args){
        //default source operation
        return line;
    }

    public void add_edge(Node dst, Integer dstport)
    {
        //this.dsts.add(new jsucuri.Edge(dst.id, dstport))
        this.dsts.add(new Edge(dst.id, dstport));
    }

    public List create_oper(Object value, Integer workerid, ArrayBlockingQueue operq, Integer tag)
    {
        List opers = new ArrayList();

        if(this.dsts.size() == 0)
        {
            opers.add(new Oper(workerid,null,null,null));
        }
        else
        {
            Iterator it = dsts.iterator();
            while(it.hasNext())
            {
                Edge edge = (Edge) it.next();
                Oper oper = new Oper(workerid, edge.dst_id, edge.dst_port, value);
                oper.tag = tag;
                opers.add(oper);
            }

            //not compatible with jdk6
            //dsts.forEach((e) -> {
            //    Edge edge = (Edge) e;
            //    Oper oper = new Oper(workerid, edge.dst_id, edge.dst_port, value);
            //    oper.tag = tag;
            //    opers.add(oper);
            //});
        }
        return opers;
    }
}
