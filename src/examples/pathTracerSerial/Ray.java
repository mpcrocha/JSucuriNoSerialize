import com.sun.javafx.geom.Vec3f;

/**
 * Created by alexandrenery on 5/26/17.
 */
public class Ray {
    private Vec3f origin;
    private Vec3f dir;

    private Ray()
    {
        this.origin = new Vec3f(0.0f,0.0f,0.0f);
        this.dir = new Vec3f(0.0f,0.0f,0.0f);
    }

    private Ray(Vec3f o, Vec3f d)
    {
        this.origin = o;
        this.dir = d;
    }

    private Ray(float ox, float oy, float oz, float dx, float dy, float dz)
    {
        this.origin = new Vec3f(ox,oy,oz);
        this.dir = new Vec3f(dx,dy,dz);
    }

    public Vec3f getOrigin()
    {
        return this.origin;
    }

    public Vec3f getDir()
    {
        return this.dir;
    }

    public static Ray createCamRay(int x_coord, int y_coord, int width, int height)
    {
        float fx = (float)x_coord / (float)width;
        float fy = (float)y_coord / (float)height;

        float aspect_ratio = (float)(width) / (float)(height);
        float fx2 = (fx - 0.5f) * aspect_ratio;
        float fy2 = fy - 0.5f;

        Vec3f pixel_pos = new Vec3f(fx2, -fy2, 0.0f);

        Vec3f o = new Vec3f(0.0f,0.0f,40.0f);
        pixel_pos.sub(o);
        pixel_pos.normalize();

        Ray r = new Ray(o, pixel_pos);

        return r;
    }

    public Ray clone()
    {
        return new Ray(new Vec3f(this.origin.x,this.origin.y,this.origin.z), new Vec3f(this.dir.x, this.dir.y, this.dir.z));
    }

    public String toString()
    {
        return "Ray(" + this.origin + "," + this.dir + ")";
    }
}


/*
        struct Ray createCamRay(const int x_coord, const int y_coord, const int width, const int height){

        float fx = (float)x_coord / (float)width;
        float fy = (float)y_coord / (float)height;

 // calculate aspect ratio
        float aspect_ratio = (float)(width) / (float)(height);
        float fx2 = (fx - 0.5f) * aspect_ratio;
        float fy2 = fy - 0.5f;

 // determine position of pixel on screen
        float3 pixel_pos = (float3)(fx2, -fy2, 0.0f);

 // create camera ray
        struct Ray ray;
        ray.origin = (float3)(0.0f, 0.0f, 40.0f);
        ray.dir = normalize(pixel_pos - ray.origin);

        return ray;
        }
*/