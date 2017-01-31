package jsucurilegacy;

import java.io.Serializable;

/**
 * Created by marcos on 01/10/16.
 */
public class TaggedValue implements Serializable {
    Object value;
    Integer tag;
    Boolean requestTask = true;

    public TaggedValue(){

    }

    public TaggedValue(Object value, Integer tag){
        this.value = value;
        this.tag = tag;

    }

    public int compare(TaggedValue taggedValue2){
        if(taggedValue2==null)
            return 1;
        if(!(this instanceof TaggedValue))
            //throw new Exception("can only compare jsucuri.TaggedValue with jsucuri.TaggedValue.");
            System.out.println("can only compare jsucuri.TaggedValue with jsucuri.TaggedValue.");
        if(this.tag > taggedValue2.tag)
            return 1;
        else if(this.tag < taggedValue2.tag)
            return -1;
        return 0;
    }

    @Override
    public String toString() {
        return this.tag+" "+this.value;
    }
}
