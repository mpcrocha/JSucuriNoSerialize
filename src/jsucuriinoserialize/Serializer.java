package jsucuriinoserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by marcos on 01/10/16.
 */
public class Serializer extends Node {

    Integer next_tag = 0;
    Object arg_buffer[];
    //default affinity to jsucuri.Worker-0 (jsucuri.Serializer HAS to be pinned)
    NodeFunction f;
    Integer affinity = 0;
    List<TagVal> inport[];
    List dsts;


    public Serializer(){

    }

    public Serializer(NodeFunction f, Integer inputn){
        this.f = f;
        if(inputn > 0){
            this.arg_buffer = new ArrayList[inputn];
            this.inport = new ArrayList[inputn];

            for (int i = 0 ; i < inputn ; i++) {
                arg_buffer[i] = new ArrayList();
                inport[i] = new ArrayList();
            }
        }
        this.dsts = new ArrayList();

    }

    public void addInport(Integer dstport, TagVal tagVal){
        inport[dstport].add(tagVal);
    }

    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq){
        if(args==null || args[0]==null){
            List opers = new ArrayList();
            opers.add(new Oper(workerid, null, null, null));
            sendops(opers, operq);
        }else{

            //print "Got operand with tag %d (expecting %d) jsucuri.Worker %d" %(args[0].tag, self.next_tag, workerid)
            //TODO wtf is that

            /*
            for (arg, argbuffer) in map(None, args, self.arg_buffer):
            bisect.insort(argbuffer, arg)
            */
            Object[] argbuffer = null;
            for(int i=0; i<arg_buffer.length; i++){
                argbuffer = new Object[arg_buffer.length];
                ArrayList lista = (ArrayList) arg_buffer[i];
                //System.out.println("arg_buffer[i]:"+arg_buffer[i]);
                //System.out.println(args[i]);
                lista.add(args[i]);
                //argbuffer = lista.toArray();
                argbuffer[i] = args[i];
            }

            if(((TaggedValue)args[0]).tag.equals(this.next_tag)){
                int next = this.next_tag;
                //argbuffer = this.arg_buffer;

                //Integer buffertag = ((jsucuri.TaggedValue)(((ArrayList)argbuffer[0]).get(0))).tag;
                Integer buffertag = ((TaggedValue)argbuffer[0]).tag;
                while (buffertag == next){
                    args = argbuffer;

                    List opers = create_oper(f.f(args), workerid, operq, 0);

                    sendops(opers, operq);
                    next += 1;
                    if(argbuffer.length> 0)
                        buffertag = ((TaggedValue)argbuffer[0]).tag;
                    else
                        buffertag = null;
                }
                this.next_tag = next;

            }
        }

    }

    public List<TagVal>[] getInport(){
        return inport;
    }
    public List getDsts(){return dsts;}

}
