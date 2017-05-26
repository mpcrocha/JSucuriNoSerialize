package examples.pathTracer;

/**
 * Created by Jano on 21/05/2017.
 */
public class PathTracerSerial {
    float EPSILON = 0.00003f;
    float PI = 3.14159265359f;

    public static void main(String[] args) {
        PathTracerSerial pathTracerSerial = new PathTracerSerial();
        //TODO receive the arguments
        int width = new Integer(args[0]);
        int height = new Integer(args[1]);
        int samples = new Integer(args[2]);//2048;
        pathTracerSerial.render(width, height, samples);

    }

    Ray createCamRay(int x_coord, int y_coord, int width, int height) {

        float fx = (float) x_coord / (float) width;  /* convert int in range [0 - width] to float in range [0-1] */
        float fy = (float) y_coord / (float) height; /* convert int in range [0 - height] to float in range [0-1] */

	/* calculate aspect ratio */
        float aspect_ratio = (float) (width) / (float) (height);
        float fx2 = (fx - 0.5f) * aspect_ratio;
        float fy2 = fy - 0.5f;

	/* determine position of pixel on screen */
        float[] pixel_pos = new float[]{fx2, -fy2, 0.0f};

	/* create camera ray*/
        Ray ray = new Ray();
        ray.setOrigin(new float[]{0.0f, 0.1f, 2.0f}); /* fixed camera position */
        float[] newPosition = new float[3];
        float[] origin = ray.getOrigin();
        newPosition[0] = pixel_pos[0] - origin[0];
        ray.setDirection(normalize(newPosition)); /* vector from camera to pixel on screen */

        return ray;
    }

    private float[] normalize(float[] array) {
        float modArray = calculateArrayModule(array);
        float[] normalizedArray = new float[]{array[0] / modArray,
                array[1] / modArray, array[2] / modArray};
        return normalizedArray;
    }

    private float calculateArrayModule(float[] array) {
        float a = array[0];
        float b = array[1];
        float c = array[2];

        float arrayModule = (float) Math.sqrt(new Float((a * a) + (b * b) + (c * c)));
        return arrayModule;
    }

    private void render(int width, int height, int samples, Sphere[] spheres, int sphere_count) {
         			/* y-coordinate of the pixel */
        //int SAMPLES = 2048;
        float[] output = new float[width * height * 3];
        for (int numPixel = 0; numPixel < width * height; numPixel++) {
            int work_item_id = numPixel;	/* the unique global id of the work item for the current pixel */
            int x_coord = work_item_id % width;			/* x-coordinate of the pixel */
            int y_coord = work_item_id / width;

            /* seeds for random number generator */
            int seed0 = x_coord;
            int seed1 = y_coord;

            Ray camray = createCamRay(x_coord, y_coord, width, height);

            /* add the light contribution of each sample and average over all samples*/
            float[] finalcolor = new float[]{0.0f, 0.0f, 0.0f};
            float invSamples = 1.0f / samples;

            for (int i = 0; i < samples; i++)
                finalcolor += multiplyArrayByScalar(trace(spheres, camray,
                        sphere_count, seed0, seed1),  invSamples);

            //if(work_item_id > 20000)
            //printf("work_item_id: %d\n", work_item_id);
            /* store the pixelcolour in the output buffer */
            //output[work_item_id] = finalcolor;
            output[(work_item_id * 3) + 0] = finalcolor[0];
            output[(work_item_id * 3) + 1] = finalcolor[1];
            output[(work_item_id * 3) + 2] = finalcolor[2];
        }
    }

    float[] subtractArray(float[] array, float[] otherArray) {
        float[] subtractedArray = new float[]{array[0] - otherArray[0],
                array[1] - otherArray[1], array[2] - otherArray[2]};
        return subtractedArray;
    }

    float[] addArray(float[] array, float[] otherArray) {
        float[] addedArray = new float[]{array[0] + otherArray[0],
                array[1] + otherArray[1], array[2] + otherArray[2]};
        return addedArray;
    }

    float[] multiplyArrayByScalar(float[] array, float scalar) {
        float[] multipliedByScalarArray = new float[]{array[0] * scalar,
                array[1] * scalar, array[2] * scalar};
        return multipliedByScalarArray;

    }

    float[] multiplyArrays(float[] array, float[] otherArray) {
        float[] multypliedArray = new float[]{array[0] * otherArray[0],
                array[1] * otherArray[1], array[2] * otherArray[2]};
        return multypliedArray;
    }

    float dot(float[] array, float[] otherArray ){
        float sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += array[i] * otherArray[i];
        }

        return sum;
    }

    /* (__global Sphere* sphere, const Ray* ray) */
    float intersect_sphere( Sphere sphere, Ray ray) /* version using local copy of sphere */
    {
        float[] rayToCenter =subtractArray(sphere.getPos(), ray.getOrigin());
        float b = dot(rayToCenter, ray.getDirection());
        float c = dot(rayToCenter, rayToCenter) - sphere.getRadius()*sphere.getRadius();
        float disc = b * b - c;

        if (disc < 0.0f)
            return 0.0f;
        else
            disc = (float)Math.sqrt(disc);

        if ((b - disc) > EPSILON) return b - disc;
        if ((b + disc) > EPSILON) return b + disc;

        return 0.0f;
    }

    boolean intersect_scene(Sphere[] spheres, Ray ray, float t, int sphere_id, int sphere_count)
    {
	/* initialise t to a very large number,
	so t will be guaranteed to be smaller
	when a hit with the scene occurs */

        float inf = 1e20f;
        t = inf;

	/* check if the ray intersects each sphere in the scene */
        for (int i = 0; i < sphere_count; i++)  {

            Sphere sphere = spheres[i]; /* create local copy of sphere */

		/* float hitdistance = intersect_sphere(&spheres[i], ray); */
            float hitdistance = intersect_sphere(sphere, ray);
		/* keep track of the closest intersection and hitobject found so far */
            if (hitdistance != 0.0f && hitdistance < t) {
                t = hitdistance;
                sphere_id = i;
            }
        }
        return t < inf; /* true when ray interesects the scene */
    }

    static float get_random( int seed0, int seed1) {

	/* hash the seeds using bitwise AND operations and bitshifts */
        seed0 = 36969 * ((seed0) & 65535) + ((seed0) >> 16);
        seed1 = 18000 * ((seed1) & 65535) + ((seed1) >> 16);

        int ires = ((seed0) << 16) + (seed1);

	/* use union struct to convert int to float */


        int ui = (ires & 0x007fffff) | 0x40000000;  /* bitwise AND, bitwise OR */
        return (new Float(ui) - 2.0f) / 2.0f;
    }


    /* the path tracing function */
/* computes a path (starting from the camera) with a defined number of bounces, accumulates light/color at each bounce */
/* each ray hitting a surface will be reflected in a random direction (by randomly sampling the hemisphere above the hitpoint) */
/* small optimisation: diffuse ray directions are calculated using cosine weighted importance sampling */

    float[] trace(Sphere[] spheres, Ray camray, int sphere_count, int seed0, int seed1) {

        Ray ray = camray;

        float[] accum_color = new float[]{0.0f, 0.0f, 0.0f};
        float[] mask = new float[]{1.0f, 1.0f, 1.0f};

        for (int bounces = 0; bounces < 8; bounces++) {

            float t;   /* distance to intersection */
            int hitsphere_id = 0; /* index of intersected sphere */

		/* if ray misses scene, return background colour */
            if (!intersect_scene(spheres, ray, t, hitsphere_id, sphere_count))
                //return accum_color += multiplyArrays(mask, new float[]{0.15f, 0.15f, 0.25f});
                return addArray(accum_color, multiplyArrays(mask, new float[]{0.15f, 0.15f, 0.25f}));
		/* else, we've got a hit! Fetch the closest hit sphere */
            Sphere hitsphere = spheres[hitsphere_id]; /* version with local copy of sphere */

		/* compute the hitpoint using the ray equation */
            float[] hitpoint = addArray(ray.getOrigin(), multiplyArrayByScalar(ray.getDirection(), t));

		/* compute the surface normal and flip it if necessary to face the incoming ray */
            float[] normal = normalize(subtractArray(hitpoint, hitsphere.getPos()));
            float[] normal_facing = dot(normal, ray.getDirection()) < 0.0f ? normal :
                    multiplyArrayByScalar(normal, (-1.0f));

		/* compute two random numbers to pick a random point on the hemisphere above the hitpoint*/
            float rand1 = 2.0f * PI * get_random(seed0, seed1);
            float rand2 = get_random(seed0, seed1);
            float rand2s = (float) Math.sqrt(rand2);

		/* create a local orthogonal coordinate frame centered at the hitpoint */
            float[] w = normal_facing;
            float[] axis = Math.abs(w[0]) > 0.1f ? new float[]{0.0f, 1.0f, 0.0f}:
                    new float[]{1.0f, 0.0f, 0.0f};
            float[] u = normalize(cross(axis, w));
            float[] v = cross(w, u);

		/* use the coordinte frame and random numbers to compute the next ray direction */
            float[]array1 = multiplyArrayByScalar(multiplyArrayByScalar(u,
                    (float)Math.cos(rand1)), rand2s);
            float[]array2 = multiplyArrayByScalar(multiplyArrayByScalar(v,
                    (float) Math.sin(rand1)), rand2s);
            float[]array3 = multiplyArrayByScalar(w, (float)Math.sqrt(1.0f - rand2));
            float[] newdir = normalize(addArray(addArray(array1, array2), array3));

		/* add a very small offset to the hitpoint to prevent self intersection */
            ray.setOrigin(addArray(hitpoint, multiplyArrayByScalar(normal_facing, EPSILON)));
            ray.setDirection(newdir);

		/* add the colour and light contributions to the accumulated colour */
            addArray(accum_color, multiplyArrays(mask, hitsphere.getEmission()));

		/* the mask colour picks up surface colours at each bounce */
            mask = multiplyArrays(mask, hitsphere.getColor());

		/* perform cosine-weighted importance sampling for diffuse surfaces*/
            mask = multiplyArrayByScalar(mask, dot(newdir, normal_facing));
        }

        return accum_color;

    }
}