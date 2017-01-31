package examples.vrc;

import java.io.Serializable;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class Ray implements Serializable
{
    Point3d o;
    Point3d t;

    public Ray(Point3d origin, Point3d target)
    {
        this.o = origin;
        this.t = target;
    }

    public Point3d getDir()
    {
        float x = t.x - o.x;
        float y = t.y - o.y;
        float z = t.z - o.z;

        Point3d res = new Point3d(x,y,z);
        res.normalize();
        return res;
    }
}