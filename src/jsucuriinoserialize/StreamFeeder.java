package jsucuriinoserialize; /**
 * Created by alexandrenery on 9/21/16.
 */

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class StreamFeeder extends Node {

    Object value;
    BufferedReader it;
    Integer tagcounter = 0;

    public StreamFeeder(Object value, BufferedReader it) {
        this.value = value;
        this.dsts = new ArrayList();
        this.affinity = null;
        this.it = it;
    }

    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq)
    {
        try {
            String line;
            while ((line = it.readLine()) != null) {
                //String result = f(line, null);
                Object value = this.f();
                Integer tag = this.tagcounter;
                List opers = create_oper(new TaggedValue(value, tag), workerid, operq, tag);
                //List opers = create_oper(value, workerid, operq, 0); //default tag = 0


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

    private Object f()
    {

        //System.out.println("Feeding " + value);
        return value;
    }


}
