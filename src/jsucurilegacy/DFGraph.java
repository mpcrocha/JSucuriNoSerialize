package jsucurilegacy; /**
 * Created by alexandrenery on 9/20/16.
 */

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DFGraph
{
    List<Node> nodes;
    public Integer node_count;

    public DFGraph()
    {
        nodes = new ArrayList<Node>();
        node_count = 0;
    }

    public void add(Node n)
    {
        n.id = this.node_count;
        this.node_count += 1;
        nodes.add(n);
    }

    public String toString()
    {
        String ret = "Node count = " + node_count + "\n";

        int cont = 0;
        Iterator<Node> it = nodes.iterator();
        while(it.hasNext())
        {
            Node n = it.next();
            ret += "Node[" + n.id +"]\n";


            for(int i = 0 ; i < n.dsts.size() ; i++) {

                ret += "(" + n.id + "->" + n.dsts.get(i).dst_id + ")\n";
            }
        }

        return ret;
    }

}

