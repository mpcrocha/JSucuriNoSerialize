import com.sun.javafx.geom.Vec3f;

/**
 * Created by alexandrenery on 5/26/17.
 */
public class Intersect {
    public boolean hit;
    public float t;
    public Vec3f hitpoint;
    public Vec3f normal;
    public float cosine_factor;
    public int sphere_id;
    public Sphere sphere;

    public Intersect()
    {
        hit = false;
        t = 1e20f;
        hitpoint = new Vec3f();
        normal = new Vec3f();
        cosine_factor = 0.0f;
        sphere_id = -1;
        sphere = null;
    }
}
