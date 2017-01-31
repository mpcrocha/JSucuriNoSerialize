package jsucuriinoserialize;

import java.io.Serializable;

/**
 * Created by alexandrenery on 9/20/16.
 */

public abstract class NodeFunction implements Serializable{
    public abstract Object f(Object[] in_operands);
}
