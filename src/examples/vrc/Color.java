package examples.vrc;

import java.io.Serializable;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class Color implements Serializable {
    public float r;
    public float g;
    public float b;
    public float alpha;

    public Color()
    {

    }

    public Color(float r, float g, float b)
    {
        this.r = r;
        this.g = g;
        this.b = b;
        this.alpha = 0.0f;
    }
}
