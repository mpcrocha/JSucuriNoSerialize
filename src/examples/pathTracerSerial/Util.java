package examples.pathTracerSerial;

/**
 * Created by alexandrenery on 6/3/17.
 */
public class Util {

//    cx = aybz − azby
//    cy = azbx − axbz
//    cz = axby − aybx

    public static Vec3f cross(Vec3f a, Vec3f b)
    {
        return new Vec3f(a.y*b.z - a.z*b.y,a.z*b.x - a.x*b.z,a.x*b.y - a.y*b.x);
    }

    public static Vec3f add(Vec3f a, Vec3f b)
    {
        return new Vec3f(a.x + b.x, a.y + b.y, a.z + b.z);
    }

    public static Vec3f mul(Vec3f a, Vec3f b)
    {
        return new Vec3f(a.x * b.x, a.y * b.y, a.z * b.z);
    }

    public static Vec3f mul(Vec3f a, float s)
    {
        return new Vec3f(a.x * s, a.y * s, a.z * s);
    }
}
