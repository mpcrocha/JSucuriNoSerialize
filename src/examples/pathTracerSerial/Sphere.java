import com.sun.javafx.geom.Vec3f;

/**
 * Created by alexandrenery on 5/26/17.
 */
public class Sphere {
    private float radius;
    private Vec3f pos;
    private Vec3f emi;
    private Vec3f color;

    public Sphere()
    {
        pos = new Vec3f();
        emi = new Vec3f();
        color = new Vec3f();
    }

    public Sphere(float pos_x, float pos_y, float pos_z, float radius)
    {
        setPosition(pos_x, pos_y, pos_z);
        setRadius(radius);
    }

    public Sphere(Vec3f pos, float radius)
    {
        setPosition(pos);
        setRadius(radius);
    }

    public void setEmi(float x, float y, float z)
    {
        this.emi.set(x,y,z);
    }

    public void setEmi(Vec3f emi)
    {
        this.emi = emi;
    }

    public Vec3f getEmi()
    {
        return this.emi;
    }

    public void setColor(Vec3f rgb)
    {
        this.color = rgb;
    }

    public void setColor(float r, float g, float b)
    {
        this.color.set(r,g,b);
    }

    public Vec3f getColor()
    {
        return this.color;
    }

    public void setRadius(float radius)
    {
        this.radius = radius;
    }

    public float getRadius()
    {
        return this.radius;
    }

    public void setPosition(Vec3f pos)
    {
        this.pos = pos;
    }

    public void setPosition(float x, float y, float z)
    {
        pos.set(x,y,z);
    }

    public Vec3f getPosition()
    {
        return this.pos;
    }

    Intersect hit(Ray ray)
    {
        Vec3f rayToCenter = new Vec3f(pos.x, pos.y, pos.z);
        rayToCenter.sub(ray.getOrigin());

        float b = rayToCenter.dot(ray.getDir());
        float c = rayToCenter.dot(rayToCenter) - getRadius()*getRadius();
        float disc = b*b - c;

        Intersect it = new Intersect();
        it.hit = false;

        it.hitpoint = new Vec3f(ray.getOrigin().x,ray.getOrigin().y, ray.getOrigin().z);
        Vec3f dir = new Vec3f(ray.getDir().x, ray.getDir().y, ray.getDir().z);

        if (disc < 0.0f) {
            it.hit = false;
            return it; //false
        }
        else {
            it.t = (float) (b - Math.sqrt(disc));
            dir.mul(it.t);
            it.hitpoint.add(dir);

            it.normal = new Vec3f(it.hitpoint.x, it.hitpoint.y, it.hitpoint.z);
            it.normal.sub(this.getPosition());
            it.normal.normalize();
            it.cosine_factor = it.normal.dot(ray.getDir())*(-1.0f);
        }

        if (it.t < 0.0f){
            it.t = (float) (b + Math.sqrt(disc));
            dir.mul(it.t);
            it.hitpoint.add(dir);

            it.normal = new Vec3f(it.hitpoint.x, it.hitpoint.y, it.hitpoint.z);
            it.normal.sub(this.getPosition());
            it.normal.normalize();
            it.cosine_factor = it.normal.dot(ray.getDir())*(-1.0f);

            if (it.t < 0.0f) {
                it.hit = false;
                return it; //false
            }
        }
        else {
            it.hit = true;
            return it;
        }
        return it;
    }


    public String toString() {
        return "Sphere(" + pos + "," + radius + ")";
    }
}
