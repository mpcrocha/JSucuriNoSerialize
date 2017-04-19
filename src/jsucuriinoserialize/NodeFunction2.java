package jsucuriinoserialize;

import java.io.Serializable;

/**
 * Created by alexandrenery on 9/20/16.
 */

public abstract class NodeFunction2 implements Serializable{
    public abstract <T extends Object> T f(T in_operands);
}
