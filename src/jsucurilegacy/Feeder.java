package jsucurilegacy; /**
 * Created by alexandrenery on 9/21/16.
 */

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

public class Feeder extends Node {

    Object value;

    public Feeder(Object value) {
        this.value = value;
        this.dsts = new ArrayList();
        this.affinity = null;
    }

    public void run(Object[] args, Integer workerid, ArrayBlockingQueue operq)
    {
        Object value = this.f();
        List opers = create_oper(value, workerid, operq,0); //default tag = 0
        sendops(opers,operq);
    }

    private Object f()
    {
        System.out.println("Feeding " + value);
        return value;
    }


}
