package jsucurilegacy;

import java.io.Serializable;

/**
 * Created by alexandrenery on 11/8/16.
 */
public abstract class LCSNodeFunction implements Serializable{
    public abstract Object[] f(Object[] in_operands, int i, int j);
}