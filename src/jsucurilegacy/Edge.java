package jsucurilegacy;

/**
 * Created by alexandrenery on 9/20/16.
 */

public class Edge
{
    public Integer dst_id;
    public Integer dst_port;
    public Integer srcport;

    public Edge(Integer dst_id, Integer dst_port)
    {
        this.dst_id = dst_id;
        this.dst_port = dst_port;
        this.srcport = 0;
    }

    public Edge(Integer dst_id, Integer dst_port, Integer srcport)
    {
        this.dst_id = dst_id;
        this.dst_port = dst_port;
        this.srcport = srcport;
    }

}

