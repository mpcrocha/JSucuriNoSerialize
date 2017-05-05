package examples.javaCLTest;

import com.nativelibs4java.opencl.CLBuffer;
import com.nativelibs4java.opencl.CLEvent;
import org.bridj.Pointer;

/**
 * Created by marcos on 03/05/17.
 */
public class ObjectCopyCL {
    CLBuffer<?> buffer;
    private CLEvent event;
    Pointer<?> pointer;

    public ObjectCopyCL() {
        this.buffer = null;
        this.event = null;
    }

    public ObjectCopyCL(CLBuffer<?> buffer, CLEvent event) {
        this.buffer = buffer;
        this.event = event;
    }

    public ObjectCopyCL(Pointer<?> pointer, CLEvent event) {
        this.pointer = pointer;
        this.event = event;
    }

    public CLBuffer<?> getBuffer() {
        return buffer;
    }

    public void setPointer(Pointer<?> pointer) {
        this.pointer = pointer;
    }

    public Pointer<?> getPointer() {
        return pointer;
    }

    public void setBuffer(CLBuffer<?> buffer) {
        this.buffer = buffer;
    }

    public CLEvent getEvent() {
        return event;
    }

    public void setEvent(CLEvent event) {
        this.event = event;
    }
}
