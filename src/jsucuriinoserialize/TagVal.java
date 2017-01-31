package jsucuriinoserialize;

/**
 * Created by alexandrenery on 9/20/16.
 */

public class TagVal
{
    Integer tag;
    Object val;

    public TagVal(Integer tag, Object val)
    {
        this.tag = tag;
        this.val = val;
    }

    public String toString()
    {
        return "(" + tag + "," + val + ")";
    }

}
