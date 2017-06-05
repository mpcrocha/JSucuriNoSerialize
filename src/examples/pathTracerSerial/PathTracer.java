import com.sun.javafx.geom.Vec3f;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Created by alexandrenery on 5/26/17.
 */
public class PathTracer {

    public static final float EPSILON = 0.00003f; /* required to compensate for limited float precision */
    public static final float PI = 3.14159265359f;
    public static final int SAMPLES = 128;

    public static int seed0;
    public static int seed1;

    private Sphere[] scene;

    public PathTracer(Sphere sphere1)
    {

        this.scene = new Sphere[9];
        initScene(scene);
    }

    void initScene(Sphere[] cpu_spheres){

        // left wall
        cpu_spheres[0] = new Sphere();
        cpu_spheres[0].setRadius(200.0f);
        cpu_spheres[0].setPosition(-200.6f, 0.0f, 0.0f);
        cpu_spheres[0].setColor(0.75f, 0.25f, 0.25f);
        cpu_spheres[0].setEmi(0.0f, 0.0f, 0.0f);

        // right wall
        cpu_spheres[1] = new Sphere();
        cpu_spheres[1].setRadius(200.0f);
        cpu_spheres[1].setPosition(200.6f, 0.0f, 0.0f);
        cpu_spheres[1].setColor(0.25f, 0.25f, 0.75f);
        cpu_spheres[1].setEmi(0.0f, 0.0f, 0.0f);

        // floor
        cpu_spheres[2] = new Sphere();
        cpu_spheres[2].setRadius(200.0f);
        cpu_spheres[2].setPosition(0.0f, -200.4f, 0.0f);
        cpu_spheres[2].setColor(0.9f, 0.8f, 0.7f);
        cpu_spheres[2].setEmi(0.0f, 0.0f, 0.0f);

        // ceiling
        cpu_spheres[3] = new Sphere();
        cpu_spheres[3].setRadius(200.0f);
        cpu_spheres[3].setPosition(0.0f, 200.4f, 0.0f);
        cpu_spheres[3].setColor(0.9f, 0.8f, 0.7f);
        cpu_spheres[3].setEmi(0.0f, 0.0f, 0.0f);

        // back wall
        cpu_spheres[4] = new Sphere();
        cpu_spheres[4].setRadius(200.0f);
        cpu_spheres[4].setPosition(0.0f, 0.0f, -200.4f);
        cpu_spheres[4].setColor(0.9f, 0.8f, 0.7f);
        cpu_spheres[4].setEmi(0.0f, 0.0f, 0.0f);

        // front wall
        cpu_spheres[5] = new Sphere();
        cpu_spheres[5].setRadius(200.0f);
        cpu_spheres[5].setPosition(0.0f, 0.0f, 202.0f);
        cpu_spheres[5].setColor(0.9f, 0.8f, 0.7f);
        cpu_spheres[5].setEmi(0.0f, 0.0f, 0.0f);

        // left sphere
        cpu_spheres[6] = new Sphere();
        cpu_spheres[6].setRadius(0.16f);
        cpu_spheres[6].setPosition(-0.25f, -0.24f, -0.1f);
        cpu_spheres[6].setColor(0.9f, 0.8f, 0.7f);
        cpu_spheres[6].setEmi(0.0f, 0.0f, 0.0f);

        // right sphere
        cpu_spheres[7] = new Sphere();
        cpu_spheres[7].setRadius(0.16f);
        cpu_spheres[7].setPosition(0.25f, -0.24f, 0.1f);
        cpu_spheres[7].setColor(0.9f, 0.8f, 0.7f);
        cpu_spheres[7].setEmi(0.0f, 0.0f, 0.0f);

        // lightsource
        cpu_spheres[8] = new Sphere();
        cpu_spheres[8].setRadius(1.0f);
        cpu_spheres[8].setPosition(0.0f, 1.36f, 0.0f);
        cpu_spheres[8].setColor(0.0f, 0.0f, 0.0f);
        cpu_spheres[8].setEmi(9.0f, 8.0f, 6.0f);

    }

    public Vec3f trace(Ray camray)
    {
        Vec3f accum_color = new Vec3f(0.0f,0.0f,0.0f);
        Vec3f mask = new Vec3f(1.0f,1.0f,1.0f);

        Ray ray = camray.clone();

        for(int bounces = 0 ; bounces < 8 ; bounces++)
        {
            float t; //distance to intersection
            int hitsphere_id = 0;

            Intersect it = intersectSpheres(ray);

            if(!it.hit)
            {
                mask.x = mask.x * 0.15f;
                mask.y = mask.y * 0.15f;
                mask.z = mask.z * 0.25f;
                accum_color.add(mask);

                return accum_color;
            }

            Sphere hitsphere = it.sphere;
            Vec3f hitpoint = it.hitpoint;
            Vec3f normal = new Vec3f(hitpoint.x,hitpoint.y,hitpoint.z);
            normal.sub(hitsphere.getPosition());
            normal.normalize();

            Vec3f normal_facing;// = new Vec3f(normal.x, normal.y, normal.z);
            if(normal.dot(ray.getDir()) < 0.0f)
            {
                normal_facing = new Vec3f(normal);
            }
            else
            {
                //normal.mul(-1.0f);
                normal_facing = new Vec3f(normal);
                normal_facing.mul(-1.0f);
            }

            float rand1 = 2.0f * PI * get_random();
            float rand2 = get_random();
            float rand2s = (float) Math.sqrt(rand2);

            Vec3f w = new Vec3f(normal_facing);

            Vec3f axis = Math.abs(w.x) > 0.1f ? new Vec3f(0.0f,1.0f,0.0f) : new Vec3f(1.0f,0.0f,0.0f);

            Vec3f u = Util.cross(axis,w);
            u.normalize();

            Vec3f v = Util.cross(w,u);

            u.mul((float) Math.cos(rand1)*rand2s);
            v.mul((float) Math.sin(rand1)*rand2s);
            w.mul((float) Math.sqrt(1.0f - rand2));

            Vec3f newdir = new Vec3f(u);
            newdir.add(v);
            newdir.add(w);
            newdir.normalize();

            Vec3f normal_facing2 = new Vec3f(normal_facing);
            normal_facing2.mul(PathTracer.EPSILON);
            normal_facing2.add(it.hitpoint);
            ray.getOrigin().set(normal_facing2);
            ray.getDir().set(newdir);

            accum_color.add(Util.mul(mask,hitsphere.getEmi()));

            mask.x *= hitsphere.getColor().x;
            mask.y *= hitsphere.getColor().y;
            mask.z *= hitsphere.getColor().z;

            mask.x *= newdir.dot(normal_facing);
            mask.y *= newdir.dot(normal_facing);
            mask.z *= newdir.dot(normal_facing);

        }
        return accum_color;
    }

    public static float get_random() {

        seed0 = 36969 * ((seed0) & 65535) + ((seed0) >> 16);
        seed1 = 18000 * ((seed1) & 65535) + ((seed1) >> 16);

        long ires = Integer.toUnsignedLong( ((seed0) << 16) + (seed1) );


	    long resui = Integer.toUnsignedLong((int) ((ires & 0x007fffff) | 0x40000000));  /* bitwise AND, bitwise OR */


        return (Float.intBitsToFloat((int) resui) - 2.0f) / 2.0f;
    }

    Intersect intersectSpheres(Ray ray)
    {
        
        Intersect res;

        res = new Intersect();
        res.t = 1e20f;
        res.hit = false;

        for(int i = 0 ; i < scene.length ; i++)
        {
            Sphere sphere = scene[i];

            Intersect it = sphere.hit(ray);

            
            if(it.hit && it.t < res.t){
                res.hit = it.hit;
                res.t = it.t;
                res.sphere_id = i;
                res.sphere = sphere;
                res.hitpoint = it.hitpoint;
                res.normal = it.normal;
                res.cosine_factor = it.cosine_factor;
            }
        }

        if(res.t < 1e20f && res.sphere != null)
            res.hit = true;

        return res;
    }

    public Vec3f[][] render(int width, int height)
    {
        Vec3f [][] output = new Vec3f[width][height];

        for(int x = 0 ; x < width ; x++)
        {
            for(int y = 0 ; y < height ; y++)
            {
                seed0 = x;
                seed1 = y;

                Ray camray = Ray.createCamRay(x,y,width,height);

                Vec3f finalColor = new Vec3f(0.0f,0.0f,0.0f);
                float invSamples = 1.0f/PathTracer.SAMPLES;

                for(int i = 0 ; i < PathTracer.SAMPLES ; i++)
                {
                    Vec3f c = trace(camray);
                    c.mul(invSamples);
                    finalColor.add(c);
                }

                output[x][y] = new Vec3f(clamp(finalColor.x), clamp(finalColor.y), clamp(finalColor.z));


            }
        }
        return output;
    }

    public float clamp(float x)
    {
        if(x < 0.0f)
            return 0.0f;

        if(x > 1.0f)
            return 1.0f;

        return x;
    }

    public static void main(String args[]) throws Exception {

        int nuberFrames = new Integer(args[0]);
        int imageWidth = new Integer(args[1]);
        int imageHeight = new Integer(args[2]);

        for(int i = 0; i < nuberFrames; i++){
            PathTracer pt = new PathTracer(new Sphere());

            Vec3f[][] output = pt.render(imageWidth, imageHeight);
            writeImage(output, i);

        }
        
        

    }

 public void writeImage(Vec3f output, int numberImage){
    BufferedImage bi = new BufferedImage(output.length, output[0].length,BufferedImage.TYPE_3BYTE_BGR);

        for(int x = 0 ; x < output.length ; x++)
        {
            for(int y = 0 ; y < output[x].length ; y++)
            {
                bi.setRGB(x,y,new Color(output[x][y].x,output[x][y].y,output[x][y].z).getRGB());
            }
        }
        ImageIO.write(bi,"png",new File("Outputs/outputPathTracerSerial_" + numberImage + ".png"));
 }

}
