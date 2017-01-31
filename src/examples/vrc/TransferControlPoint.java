package examples.vrc;

import java.io.Serializable;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class TransferControlPoint implements Serializable{
    public Color color;
    public int IsoValue;

    public TransferControlPoint(float r, float g, float b, int isovalue)
    {
        color = new Color();

        color.r = r;
        color.g = g;
        color.b = b;
        //color.W = 1.0f;
        IsoValue = isovalue;
    }

    public TransferControlPoint(float alpha, int isovalue)
    {
        color.r = 0.0f;
        color.g = 0.0f;
        color.b = 0.0f;
        //Color.W = alpha;
        IsoValue = isovalue;
    }

}
