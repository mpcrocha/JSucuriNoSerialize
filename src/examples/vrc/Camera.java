package examples.vrc;

import java.io.Serializable;
import java.util.*;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class Camera implements Serializable{

    private int width;
    private int height;
    private Point3d eye;
    private Point3d lookat;

    /*private Point3d u;
    private Point3d v;
    private Point3d w;
    private Point3d up;
    private float distance;*/

    public static final int NRAN = 1024;
    public static final int MASK = NRAN - 1;
    public static final int RAY_MAG = 1000;
    public static final float FOV = (float) Math.PI /4.0f;
    public static final float HALF_FOV = (float) 0.5f*FOV;
    public static final int RAND_MAX = 32767;

    private float aspect;

    private Point3d urand[];
    private int irand[];

    public Camera()
    {

    }

    public Camera(int width, int height, Point3d eye, Point3d lookat)
    {
        this.width = width;
        this.height = height;
        this.eye = eye;
        this.lookat = lookat;

        /*up = new Point3d(0.0f,1.0f,0.0f);
        u = new Point3d();
        v = new Point3d();
        w = new Point3d();

        distance = Point3d.p2p_distance(eye,lookat);*/
        aspect = width/height;

        urand = new Point3d[NRAN];
        irand = new int[NRAN];

        for(int i = 0 ; i < urand.length ; i++)
        {
            urand[i] = new Point3d();
            irand[i] = 0;
        }

        Random rand = new Random();

        for(int i=0; i<NRAN; i++) urand[i].x = ((float) ((float) rand.nextInt(RAND_MAX) / (float) RAND_MAX - 0.5f));
        for(int i=0; i<NRAN; i++) urand[i].y = ((float) ((float) rand.nextInt(RAND_MAX) / (float) RAND_MAX - 0.5f));
        for(int i=0; i<NRAN; i++) irand[i] = (int)(NRAN * ((float) rand.nextInt(RAND_MAX) / (float) RAND_MAX));

        //setupCamera();



    }

    public int getWidth()
    {
        return width;
    }

    public int getHeight()
    {
        return height;
    }

    /*public void setupCamera()
    {
        w = new Point3d(eye.x - lookat.x, eye.y - lookat.y, eye.z - lookat.z);
        w.normalize();

        u = Point3d.crossProduct(w, up);
        u.normalize();

        v = Point3d.crossProduct(u, w);
        v.normalize();
    }*/

    public Ray get_primary_ray( int x, int y,  int sample ) {
        Ray ray;
        float m[][] = new float[3][3];

        Point3d i, j , k, dir;
        j = new Point3d(0.0f,1.0f,0.0f);
        i = new Point3d();
        k = new Point3d();

        k.x = ( lookat.x - eye.x );
        k.y = ( lookat.y - eye.y );
        k.z = ( lookat.z - eye.z );

        k.normalize();

        i = Point3d.crossProduct(j, k);
        j = Point3d.crossProduct(k, i);

        m[0][0] = i.x; m[0][1] = j.x; m[0][2] = k.x;
        m[1][0] = i.y; m[1][1] = j.y; m[1][2] = k.y;
        m[2][0] = i.z; m[2][1] = j.z; m[2][2] = k.z;

        //printTable(m);

        Point3d o = new Point3d(0.0f,0.0f,0.0f);
        Point3d d = new Point3d(0.0f,0.0f,0.0f);

        //ray.o.x = ray.o.y = ray.o.z = 0.0;

        d = get_sample_pos(x, y, sample);

        d.z = (1.0f/HALF_FOV);
        d.x = (d.x * RAY_MAG);
        d.y = (d.y * RAY_MAG);
        d.z = (d.z * RAY_MAG);

        dir = new Point3d(d.x + o.x, d.y + o.y, d.z + o.z);

        float fooX = dir.x * m[0][0] + dir.y * m[0][1] + dir.z * m[0][2];
        float fooY = dir.x * m[1][0] + dir.y * m[1][1] + dir.z * m[1][2];
        float fooZ = dir.x * m[2][0] + dir.y * m[2][1] + dir.z * m[2][2];

        float origX = o.x * m[0][0] + o.y * m[0][1] + o.z * m[0][2] + eye.x;
        float origY = o.x * m[1][0] + o.y * m[1][1] + o.z * m[1][2] + eye.y;
        float origZ = o.x * m[2][0] + o.y * m[2][1] + o.z * m[2][2] + eye.z;

        o.x = (origX);
        o.y = (origY);
        o.z = (origZ);

        d.x = (fooX + origX);
        d.y = (fooY + origY);
        d.z = (fooZ + origZ);

        return new Ray(o,d);
    }


    public Point3d get_sample_pos(int x, int y, int sample) {
        Point3d pt = new Point3d();
	/*float xsz = 2.0, ysz = WID / aspect;*/

        //static float sf = 0.0;

        float sf = 0.0f;

        if(sf == 0.0f) {
            sf = 2.0f / (float)width;
        }

        pt.x = ( ((float)x / (float)width) - 0.5f );
        pt.y = ( -(((float)y / (float)height) - 0.65f) / (float) aspect);

        if(sample != 0) {
            Point3d jt = jitter(x, y, sample);
            //pt.x += jt.x * sf;
            pt.x = (pt.x + jt.x * sf);

            //pt.y += jt.y * sf / aspect;
            pt.y = (pt.y + jt.y * sf / (float) aspect);
        }
        return pt;
    }

    /* jitter function taken from Graphics Gems I. */
    Point3d jitter(int x, int y, int s) {
        Point3d pt = new Point3d();
        pt.x = ( urand[(x + (y << 2) + irand[(x + s) & MASK]) & MASK].x );
        pt.y = ( urand[(y + (x << 2) + irand[(y + s) & MASK]) & MASK].y );
        return pt;
    }




}
