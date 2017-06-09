package examples.pathTracerSerial;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Created by alexandrenery on 5/26/17.
 */
public class PathTracer {

    public static final float EPSILON = 0.00003f; /* required to compensate for limited float precision */
    public static final float PI = 3.14159265359f;
    public static int SAMPLES = 128;

    public static int seed0;
    public static int seed1;

    //private Sphere[] scene;
    private World w;

    public PathTracer()
    {
        //this.scene = new Sphere[9];
        w = new World();
        w.initScene();
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

            //Intersect it = intersectSpheres(ray);
            Intersect it = w.hit(ray);
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

        //long ires = Integer.toUnsignedLong( ((seed0) << 16) + (seed1) );
        long ires = ((seed0) << 16) + (seed1) & 0x00000000ffffffffL;

	    //long resui = Integer.toUnsignedLong((int) ((ires & 0x007fffff) | 0x40000000));  /* bitwise AND, bitwise OR */
        long resui = ((int)(ires & 0x007fffff) | 0x40000000) & 0x00000000ffffffffL;
        return (Float.intBitsToFloat((int) resui) - 2.0f) / 2.0f;
    }

    public void updateScene()
    {
        w.updateScene();
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

    public static void main(String args[]) throws Exception
    {

        int numFrames = new Integer(args[0]);
        int imWidth = new Integer(args[1]);
        int imHeigth = new Integer(args[2]);
        SAMPLES = new Integer(args[3]);

        PathTracer pt = new PathTracer();

        for(int i = 0; i< numFrames; i++){
            Vec3f[][] output = pt.render(imWidth, imHeigth);
            writeImage(output, i);
            pt.updateScene();
        }

    }

    public static void writeImage(Vec3f[][] output, int numFrame){
        BufferedImage bi = new BufferedImage(output.length, output[0].length,BufferedImage.TYPE_3BYTE_BGR);

        for(int x = 0 ; x < output.length ; x++)
        {
            for(int y = 0 ; y < output[x].length ; y++)
            {
                bi.setRGB(x,y,new Color(output[x][y].x,output[x][y].y,output[x][y].z).getRGB());

            }
        }
        try {
            ImageIO.write(bi,"png",new File("Outputs/outputPathTracerSerial_"+ numFrame+ ".png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
