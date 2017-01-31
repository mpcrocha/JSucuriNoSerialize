package examples.vrc;

import java.io.Serializable;

/**
 * Created by alexandrenery on 10/26/16.
 */

public class Point3d implements Serializable
{
    float x;
    float y;
    float z;

    public Point3d()
    {
        this.x = 0.0f;
        this.y = 0.0f;
        this.z = 0.0f;
    }

    public Point3d(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float distance()
    {
        return (float) Math.sqrt(this.x*this.x + this.y*this.y + this.z*this.z);
    }

    public static float distance(Point3d p)
    {
        return (float) Math.sqrt(p.x*p.x + p.y*p.y + p.z*p.z);
    }

    public static float p2p_distance(Point3d p1, Point3d p2)
    {
        float dif1 = p2.x - p1.x;
        float dif2 = p2.y - p1.y;
        float dif3 = p2.z - p1.z;

        return (float) Math.sqrt(dif1*dif1 + dif2*dif2 + dif3*dif3);
    }

    public void normalize()
    {
        float m = distance();
        x = x/m;
        y = y/m;
        z = z/m;
    }

    public static Point3d normalize(Point3d p)
    {
        float m = distance(p);
        Point3d res = new Point3d(p.x/m, p.y/m, p.z/m);
        return res;
    }

    public static Point3d crossProduct(Point3d a, Point3d b)
    {
        float x = a.y*b.z - a.z*b.y;
        float y = a.z*b.x - a.x*b.z;
        float z = a.x*b.y - a.y*b.x;
        Point3d res = new Point3d(x,y,z);
        return res;
    }

    public static float dotProduct(Point3d a, Point3d b)
    {
        return a.x*b.x + a.y*b.y + a.z*b.z;
    }

    public void scale(float s)
    {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

}