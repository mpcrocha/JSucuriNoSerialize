package examples.vrc;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by alexandrenery on 10/26/16.
 */
public class Grid implements Serializable{
    private Point3d t_min;
    private Point3d t_max;
    private Point3d p0;
    private Point3d p1;
    private int nx, ny, nz;

    public Point3d getP0() {
        return p0;
    }

    public Point3d getP1() {
        return p1;
    }

    public int getNx() {
        return nx;
    }

    public int getNy() {
        return ny;
    }

    public int getNz() {
        return nz;
    }

    private int total;
    private float t0;
    private float t1;
    private Color black;
    private Color white;

    public static final int MAX_DEPTH = 10000;

    //private ArrayList<TransferControlPoint> colorKnots;
    private ArrayList<Point3d> alphaKnots;

    public Grid()
    {

    }

    public Grid(Point3d min, Point3d max, int nx, int ny, int nz) {

        p0 = min;
        p1 = max;

        this.nx = nx;
        this.ny = ny;
        this.nz = nz;
        total = nx * ny * nz;

        t_min = new Point3d();
        t_max = new Point3d();

        t0 = 0;
        t1 = 0;

        black = new Color();
        black.r = 0;
        black.g = 0;
        black.b = 0;

        white = new Color();
        white.r = 1.0f;
        white.g = 1.0f;
        white.b = 1.0f;

        alphaKnots = new ArrayList<Point3d>();

        alphaKnots.add(new Point3d(0, 0,0));
        alphaKnots.add(new Point3d(40,0.2f,0));
        alphaKnots.add(new Point3d(60,0.4f,0));
        alphaKnots.add(new Point3d(63,0.6f,0));
        alphaKnots.add(new Point3d(150,0.1f,0));
        alphaKnots.add(new Point3d(256, 1.0f,0));

    }

    private boolean inside_grid(Point3d p) {
        if ((p.x > p0.x) && (p.x < p1.x)) {
            if ((p.y > p0.y) && (p.y < p1.y)) {
                if ((p.z > p0.z) && (p.z < p1.z)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean intersectBoundingBox(Ray r) {

        float a;
        float b;
        float c;

        Point3d rdir = r.getDir();

        a = 1.0f / rdir.x;
        b = 1.0f / rdir.y;
        c = 1.0f / rdir.z;

        if (a >= 0.0f) {
            t_min.x = ((p0.x - r.o.x) * a);
            t_max.x = ((p1.x - r.o.x) * a);
        } else {
            t_min.x = ((p1.x - r.o.x) * a);
            t_max.x = ((p0.x - r.o.x) * a);
        }

        if (b >= 0.0f) {
            t_min.y = ((p0.y - r.o.y) * b);
            t_max.y = ((p1.y - r.o.y) * b);
        } else {
            t_min.y = ((p1.y - r.o.y) * b);
            t_max.y = ((p0.y - r.o.y) * b);
        }

        if (c >= 0.0f) {
            t_min.z = ((p0.z - r.o.z) * c);
            t_max.z = ((p1.z - r.o.z) * c);
        } else {
            t_min.z = ((p1.z - r.o.z) * c);
            t_max.z = ((p0.z - r.o.z) * c);
        }

        /*System.out.print("t_min->x:"+t_min.x +
                "t_min->y:" +t_min.y +
        "t_min->z:" + t_min.z + "\n" +
        "t_max->x:" + t_max.x +
        "t_max->y:" + t_max.y +
        "t_max->z:" + t_max.z);*/
        t0 = Math.max(Math.max(t_min.x, t_min.y), t_min.z);
        t1 = Math.min(Math.min(t_max.x, t_max.y), t_max.z);
        System.out.println("T0: " + t0 + " " + "T1: " + t1);
        if (t0 < t1) {
            System.out.println("t0 < t1");
            return true;
        }

        return false;
    }

    private float clamp(float x, float min, float max) {
        return (x < min ? min : (x > max ? max : x));
    }

    public Color intersectGrid(Ray r, float[] data, float brightness) {
        float ep = 0.11f; /*error correction for single precision*/
        int iter;
        int ix, iy, iz;
        int index;
        //point t_max;
        //point t_min;
        Point3d rd;

        float dtx;
        float dty;
        float dtz;

        float tx_next, ty_next, tz_next;
        int ix_step, iy_step, iz_step;
        int ix_stop, iy_stop, iz_stop;

        Color src, dst;
        Color tr;

        rd = r.getDir();

        /*intersect the bounding box of the volume (virtually)*/
        if (intersectBoundingBox(r)) {

            t0 = t0 - ep;

            Point3d p = new Point3d();

            p.x = (r.o.x + t0 * rd.x);
            p.y = (r.o.y + t0 * rd.y);
            p.z = (r.o.z + t0 * rd.z);


            if (inside_grid(p)) {
                ix = (int) (Math.floor(clamp((r.o.x - p0.x) * (float) nx / (p1.x - p0.x), (float) 0, (float) nx - 1)));
                iy = (int) (Math.floor(clamp((r.o.y - p0.y) * (float) ny / (p1.y - p0.y), (float) 0, (float) ny - 1)));
                iz = (int) (Math.floor(clamp((r.o.z - p0.z) * (float) nz / (p1.z - p0.z), (float) 0, (float) nz - 1)));
            } else {
                ix = (int) (Math.floor(clamp((p.x - p0.x) * (float) nx / (p1.x - p0.x), 0, (float) nx - 1)));
                iy = (int) (Math.floor(clamp((p.y - p0.y) * (float) ny / (p1.y - p0.y), 0, (float) ny - 1)));
                iz = (int) (Math.floor(clamp((p.z - p0.z) * (float) nz / (p1.z - p0.z), 0, (float) nz - 1)));

            }

            dtx = (t_max.x - t_min.x) / (float) nx;
            dty = (t_max.y - t_min.y) / (float) ny;
            dtz = (t_max.z - t_min.z) / (float) nz;

            if (rd.x > 0.0f) {
                tx_next = t_min.x + (ix + 1) * dtx;
                ix_step = +1;
                ix_stop = nx;
            } else {
                tx_next = t_min.x + (nx - ix) * dtx;
                ix_step = -1;
                ix_stop = -1;
            }

            if (rd.y > 0.0f) {
                ty_next = t_min.y + (iy + 1) * dty;
                iy_step = +1;
                iy_stop = ny;
            } else {
                ty_next = t_min.y + (ny - iy) * dty;
                iy_step = -1;
                iy_stop = -1;
            }

            if (rd.z > 0.0f) {
                tz_next = t_min.z + (iz + 1) * dtz;
                iz_step = +1;
                iz_stop = nz;
            } else {
                tz_next = t_min.z + (nz - iz) * dtz;
                iz_step = -1;
                iz_stop = -1;
            }

            src = new Color();
            dst = new Color();

            src.r = 0.0f;
            src.g = 0.0f;
            src.b = 0.0f;
            src.alpha = 0.0f;

            dst.r = 0.0f;
            dst.g = 0.0f;
            dst.b = 0.0f;
            dst.alpha = 0.0f;

            iter = 0;
            /*HERE GOES THE VOLUME RAY-CASTING*/
            while (iter < MAX_DEPTH) {
                index = ix + nx * iy + nx * ny * iz;

                src.r = data[index];
                src.g = data[index];
                src.b = data[index];
                src.alpha = data[index];

                src.alpha *= 0.05f;
                src.r *= src.alpha;
                src.g *= src.alpha;
                src.b *= src.alpha;

                dst.r = (1.0f - dst.alpha) * src.r + dst.r;
                dst.g = (1.0f - dst.alpha) * src.g + dst.g;
                dst.b = (1.0f - dst.alpha) * src.b + dst.b;
                dst.alpha = (1.0f - dst.alpha) * src.alpha + dst.alpha;

                if (dst.alpha >= 0.95f) {
                    return dst;
                }

                iter++;

                /* X-AXIS */
                if (tx_next < ty_next && tx_next < tz_next) {

                    tx_next += dtx;
                    ix += ix_step;

                    /*OUT OF THE BOUNDING BOX*/
                    if (ix == ix_stop) {
                        dst.r *= brightness;
                        dst.g *= brightness;
                        dst.b *= brightness;
                        return dst; /*color*/
                    }
                } else {
                    if (ty_next < tz_next) {

                        ty_next += dty;
                        iy += iy_step;

                        /*OUT OF THE BOUNDING BOX*/
                        if (iy == iy_stop) {
                            dst.r *= brightness;
                            dst.g *= brightness;
                            dst.b *= brightness;

                            return dst; /*color*/
                        }
                    } else {
                        tz_next += dtz;
                        iz += iz_step;

                        /*OUT OF THE BOUNDING BOX*/
                        if (iz == iz_stop) {
                            dst.r *= brightness;
                            dst.g *= brightness;
                            dst.b *= brightness;

                            return dst; /*color*/
                        }

                    }
                }
            }
        }

        return black;
    }



    Color simpleTF(float alpha) {
        Color result = new Color();
        result.r = alpha;
        result.g = alpha;
        result.b = alpha;
        result.alpha = 0.0f;

        /*foot*/

        if (alpha > 0.58f && alpha < 1.0f) {
            result.r = alpha * 0.4f;
            result.g = alpha * 0.8f;
            result.b = alpha * 0.8f;
            result.alpha = 0.2f;
        }

        if (alpha > 0.0f && alpha < 0.58f) {
            result.r = alpha * 0.6f;
            result.g = alpha * 0.3f;
            result.b = alpha * 0.3f;
            result.alpha = 0.08f;
        }
        return result;
    }

    public float interpolateAlpha()
    {


        return 0.0f;
    }

//    public String toString()
//    {
//        String res;
//        res = "GRID: \n";
//        res += "t_min = " + t_min + "\n";
//        res += "t_max = " + t_max + "\n";
//        res += "p0 = " + p0 + "\n";
//        res += "p1 = " + p1 + "\n";
//        res += "nx,ny,nz = " + nx + "," + ny + "," + nz + "\n";
//        res += "t0,t1 = " + t0 + "," + t1 + "\n";
//        return res;
//    }


}
