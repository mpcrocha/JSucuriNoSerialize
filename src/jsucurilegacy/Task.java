package jsucurilegacy; /**
 * Created by alexandrenery on 9/20/16.
 */

import java.io.Serializable;

public class Task implements Serializable
{
    Integer nodeid;
    Object[] args;
    NodeFunction nf;

    public Task(NodeFunction nf, Integer nodeid, Object[] args)
    {
        this.nf = nf;
        this.nodeid = nodeid;
        this.args = args;
    }


}

