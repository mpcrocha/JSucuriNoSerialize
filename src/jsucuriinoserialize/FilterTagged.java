package jsucuriinoserialize;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Created by marcos on 01/10/16.
 */
public class FilterTagged extends Node {

    public FilterTagged(NodeFunction nf, Integer inputn)
    {
        super(nf, inputn);
        this.nf = nf;

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
    }

    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq){
        if(args[0] == null) {
            List opers = new ArrayList();
            opers.add(new Oper(workerid, null, null, null));
            sendops(opers, operq);
        }else{
            int tag = ((TaggedValue)args[0]).tag;
            Object[] argValues = new Object[args.length];
            for(int i=0;i<args.length;i++){
                argValues[i] = ((TaggedValue)args[i]).value;
            }
            Object result = nf.f(argValues);
            List opers = create_oper(new TaggedValue(result, tag), workerid, operq, tag);
            sendops(opers, operq);
        }
    }
}
