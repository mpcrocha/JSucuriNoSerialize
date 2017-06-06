package examples.pathTracerSerial;

/**
 * Created by alexandrenery on 6/5/17.
 */
public class Vec3f {
    public float x;
    public float y;
    public float z;

    public Vec3f()
    {
        x = 0;
        y = 0;
        z = 0;
    }

    public Vec3f(Vec3f f)
    {
        this.x = f.x;
        this.y = f.y;
        this.z = f.z;
    }

    public Vec3f(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(float x, float y, float z)
    {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(Vec3f v)
    {
        this.x = v.x;
        this.y = v.y;
        this.z = v.z;
    }

    public void add(Vec3f v)
    {
        this.x += v.x;
        this.y += v.y;
        this.z += v.z;
    }

    public void sub(Vec3f v)
    {
        this.x -= v.x;
        this.y -= v.y;
        this.z -= v.z;
    }

    public void mul(Vec3f v)
    {
        this.x *= v.x;
        this.y *= v.y;
        this.z *= v.z;
    }

    public void mul(float s) {
        this.x *= s;
        this.y *= s;
        this.z *= s;
    }

    public float dot(Vec3f v)
    {
        return (this.x*v.x + this.y*v.y + this.z*v.z);
    }

    public void cross(Vec3f a, Vec3f b)
    {
        a.x = a.y*b.z - a.z*b.y;
        a.y = a.z*b.x - a.x*b.z;
        a.z = a.x*b.y - a.y*b.x;
    }

    public void normalize()
    {
        float m = (float) Math.sqrt(this.x*this.x + this.y*this.y + this.z*this.z);
        this.x = this.x/m;
        this.y = this.y/m;
        this.z = this.z/m;
    }
}
