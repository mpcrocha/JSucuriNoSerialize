package jsucuriinoserialize;

/**
 * Created by alexandrenery on 9/20/16.
 */

public class Oper implements Comparable<Oper>
{
    public Integer wid; //id of the worker that produced the oper
    public Integer dstid; //id of the target task
    public Integer dstport; //input port of the target task
    public Object value; //actual value of the operand
    public Integer tag;
    public boolean request_task;

    public Oper(Integer wid, Integer dstid, Integer dstport, Object value)
    {
        this.wid = wid;
        this.dstid = dstid;
        this.dstport = dstport;
        this.value = value;

        this.tag = 0; //default tag
        this.request_task = true; //if true, piggybacks a request for a task to the worker where the opers were produced
    }

/*
    @Override
    public int compare(jsucuri.Oper o1, jsucuri.Oper o2)
    {
        if(o1.tag > o2.tag)
        {
            return 1;
        }
        else if(o1.tag < o2.tag)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }
*/

    public int compareTo(Oper other)
    {
        if(!(other instanceof Oper))
        {
            System.out.println("can only compare jsucuri.Oper with jsucuri.Oper.");
        }
        if(this.tag > other.tag)
        {
            return 1;
        }
        else if(this.tag < other.tag)
        {
            return -1;
        }
        else
        {
            return 0;
        }
    }

    /*
    public String toString()
    {
        return "" + this.tag;
    }

    public static void main(String args[])
    {
        jsucuri.Oper o1 = new jsucuri.Oper(0,0,0,null);
        jsucuri.Oper o2 = new jsucuri.Oper(0,0,0,null);
        jsucuri.Oper o3 = new jsucuri.Oper(0,0,0,null);

        o1.tag = 0;
        o2.tag = 9;
        o3.tag = 2;

        List<jsucuri.Oper> l = new ArrayList<jsucuri.Oper>();

        l.add(o1);
        l.add(o2);
        l.add(o3);


        for(int i = 0 ; i < l.size() ; i++)
        {
            System.out.println(l.get(i));
        }

    }
*/


}

