#define TRUE (0==0)
#define FALSE (0==1)

#define MAX_BYTE 255
#define MAX_DEPTH 100000

#define SCALE_RATE 100.0f

#define PI ((float) (355.0f/113.0f))
#define FOV			((float) (PI/4.0f))		/* field of view in rads (pi/4) */
#define HALF_FOV		(FOV * 0.5f)
#define NRAN	1024
#define MASK	(NRAN - 1)

#define RAY_MAG 1000.0f

struct c_color
{
    float r;
    float g;
    float b;
    float alpha;

}typedef color;

struct p_point
{
    float x;
    float y;
    float z;
}typedef point;

struct r_ray
{
    point o;
    point d;
}typedef ray;

struct v_viewport
{
	int width;
	int height;
}typedef viewport;

struct c_camera
{
	point eye;
	point lookat;
	point up,u,v,w;
	float distance;
	viewport view;
}typedef camera;

struct g_grid
{
	point p0;
	point p1;
	int nx, ny, nz;
	int total;
}typedef grid;



void normalize_point(point *a)
{
	float m = a->x*a->x + a->y*a->y + a->z*a->z;
	m = sqrt(m);
	a->x = a->x/m;
	a->y = a->y/m;
	a->z = a->z/m;
}

void crossProduct(point *a, point *b, point *r)
{
	r->x = a->y*b->z - a->z*b->y;
	r->y = a->z*b->x - a->x*b->z;
	r->z = a->x*b->y - a->y*b->x;
}

point jitter(int x, int y, int s, __global int *irand, __global float *urand_x, __global float *urand_y) {
	point pt;
	pt.x = urand_x[(x + (y << 2) + irand[(x + s) & MASK]) & MASK];
	pt.y = urand_y[(y + (x << 2) + irand[(y + s) & MASK]) & MASK];
	return pt;
}

int inside_grid(__local grid *gr, point * p) {
	if ((p->x > gr->p0.x) && (p->x < gr->p1.x)) {
		if ((p->y > gr->p0.y) && (p->y < gr->p1.y)) {
			if ((p->z > gr->p0.z) && (p->z < gr->p1.z)) {
				return TRUE;
			}
		}
	}
	return FALSE;
}

point get_sample_pos(int x, int y, int sample, __global int *irand,
__global float *urand_x, __global float *urand_y, int width, int height)  {
	point pt;

    float sf = 2.0f/(float)width;
	/*to-do: move aspect away from here*/
    float aspect =  (float)width/(float)height;

    pt.x = ((float)x / (float)width) - 0.5f;
    pt.y = -(((float)y / (float)height) - 0.65f) / aspect;

	if(sample) {
	    printf("sample");
        point jt = jitter(x, y, sample, irand, urand_x, urand_y);
		pt.x += jt.x * sf;
		pt.y += jt.y * sf / aspect;
	}

	return pt;
}

ray get_primary_ray(__local camera *cam, int x, int y, int sample, __global int *irand, __global float *urand_x,
__global float *urand_y, int width, int height) {
	ray ray;
	float m[3][3];
	point i, j = {0.0f, 1.0f, 0.0f}, k, dir, orig, foo;

	k.x = cam->lookat.x - cam->eye.x;
	k.y = cam->lookat.y - cam->eye.y;
	k.z = cam->lookat.z - cam->eye.z;

	normalize_point(&k);

	crossProduct(&j,&k,&i);
	crossProduct(&k,&i,&j);

	m[0][0] = i.x;  
	m[1][0] = i.y;  
	m[2][0] = i.z;  
	m[0][1] = j.x;
	m[1][1] = j.y;
	m[2][1] = j.z;
	m[0][2] = k.x;
	m[1][2] = k.y;
	m[2][2] = k.z;

	ray.o.x = ray.o.y = ray.o.z = 0.0f;

	ray.d = get_sample_pos(x, y, 0, irand, urand_x, urand_y, width, height);
    	
	ray.d.z = 1.0f / HALF_FOV;
	ray.d.x *= RAY_MAG;
	ray.d.y *= RAY_MAG;
	ray.d.z *= RAY_MAG;

    dir.x = ray.d.x + ray.o.x;
	dir.y = ray.d.y + ray.o.y;
	dir.z = ray.d.z + ray.o.z;

    foo.x = dir.x * m[0][0] + dir.y * m[0][1] + dir.z * m[0][2];
	foo.y = dir.x * m[1][0] + dir.y * m[1][1] + dir.z * m[1][2];
	foo.z = dir.x * m[2][0] + dir.y * m[2][1] + dir.z * m[2][2];

	orig.x = ray.o.x * m[0][0] + ray.o.y * m[0][1] + ray.o.z * m[0][2] + cam->eye.x;
	orig.y = ray.o.x * m[1][0] + ray.o.y * m[1][1] + ray.o.z * m[1][2] + cam->eye.y;
	orig.z = ray.o.x * m[2][0] + ray.o.y * m[2][1] + ray.o.z * m[2][2] + cam->eye.z;

	ray.o = orig;
	ray.d.x = foo.x + orig.x;
	ray.d.y = foo.y + orig.y;
	ray.d.z = foo.z + orig.z;

	return ray;
}

float m_min(float a, float b)
{
	return a<b?a:b;
}

float m_max(float a, float b)
{
	return a>b?a:b;
}

void intersectBoundingBox(__local grid * gr, ray * raio, point *t_min, point *t_max, float *t0, float *t1)
{
	point rd;
    	float a;
    	float b;
    	float c;

	rd.x = raio->d.x - raio->o.x;
	rd.y = raio->d.y - raio->o.y;
	rd.z = raio->d.z - raio->o.z;
	normalize_point(&rd);

	a = (1.0f/rd.x);
	b = (1.0f/rd.y);
	c = (1.0f/rd.z);

	if (a >= 0.0f) {
		t_min->x = (gr->p0.x - raio->o.x)*a;
		t_max->x = (gr->p1.x - raio->o.x)*a;
	} else {
		t_min->x = (gr->p1.x - raio->o.x)*a;
		t_max->x = (gr->p0.x - raio->o.x)*a;
	}

	if (b >= 0.0f) {
		t_min->y = (gr->p0.y - raio->o.y)*b;
		t_max->y = (gr->p1.y - raio->o.y)*b;
	} else {
		t_min->y = (gr->p1.y - raio->o.y)*b;
		t_max->y = (gr->p0.y - raio->o.y)*b;
	}

	if (c >= 0.0f) {
		t_min->z = (gr->p0.z - raio->o.z)*c;
		t_max->z = (gr->p1.z - raio->o.z)*c;
	} else {
		t_min->z = (gr->p1.z - raio->o.z)*c;
		t_max->z = (gr->p0.z - raio->o.z)*c;
	}

	*t0 = m_max(m_max(t_min->x, t_min->y), t_min->z);
	*t1 = m_min(m_min(t_max->x, t_max->y), t_max->z);
}

color simpleTF(float alpha)
{
	color result;
	result.r = alpha;
	result.g = alpha;
	result.b = alpha;
	result.alpha = 0.0f;

	/*foot*/

	if(alpha > 0.48f && alpha < 1.0f)
	{
		result.r = alpha*0.4f;
		result.g = alpha*0.8f;
		result.b = alpha*0.8f;
		result.alpha = 0.2f;
	}

	if(alpha > 0.0f && alpha < 0.48f)
	{
		result.r = alpha*0.6f;
		result.g = alpha*0.3f;
		result.b = alpha*0.3f;
		result.alpha = 0.08f;
	}

	return result;
}

float clamp_ray_cast(float x, float min, float max) {
	return (x < min ? min : (x > max ? max : x));
} 

color intersectGrid(__local grid * gr, ray * raio, __global float *data, float brightness) {
	float ep = 0.11f; /*error correction for single precision*/
	int iter;
	int ix, iy, iz;
	int index;
	color black;
	color white;
	point t_max;
	point t_min;
	point rd;
	float t0,t1;

	float dtx;
	float dty;
	float dtz;

	float tx_next, ty_next, tz_next;
	int ix_step, iy_step, iz_step;
	int ix_stop, iy_stop, iz_stop;

	color src,dst;
	color tr;

	black.r = 0.0f;
	black.g = 0.0f;
	black.b = 0.0f;
	black.alpha = 0.0f;

	white.r = 255.0f;
	white.g = 255.0f;
	white.b = 255.0f;
	white.alpha = 0.0f;

	rd.x = raio->d.x - raio->o.x;
	rd.y = raio->d.y - raio->o.y;
	rd.z = raio->d.z - raio->o.z;
	normalize_point(&rd);

	intersectBoundingBox(gr,raio,&t_min, &t_max, &t0,&t1);

	if(t0 < t1)
	{
		point p;

		t0 = t0 - ep;

		p.x = raio->o.x + t0 * rd.x;
		p.y = raio->o.y + t0 * rd.y;
		p.z = raio->o.z + t0 * rd.z;

		if (inside_grid(gr,&p)) {
			ix = (int) (floor(clamp_ray_cast((raio->o.x - gr->p0.x) * gr->nx / (gr->p1.x - gr->p0.x), 0, gr->nx - 1)));
			iy = (int) (floor(clamp_ray_cast((raio->o.y - gr->p0.y) * gr->ny / (gr->p1.y - gr->p0.y), 0, gr->ny - 1)));
			iz = (int) (floor(clamp_ray_cast((raio->o.z - gr->p0.z) * gr->nz / (gr->p1.z - gr->p0.z), 0, gr->nz - 1)));
		} else {
			ix = (int) (floor(clamp_ray_cast((p.x - gr->p0.x) * gr->nx / (gr->p1.x - gr->p0.x), 0, gr->nx - 1)));
			iy = (int) (floor(clamp_ray_cast((p.y - gr->p0.y) * gr->ny / (gr->p1.y - gr->p0.y), 0, gr->ny - 1)));
			iz = (int) (floor(clamp_ray_cast((p.z - gr->p0.z) * gr->nz / (gr->p1.z - gr->p0.z), 0, gr->nz - 1)));
		}
		dtx = (t_max.x - t_min.x)/gr->nx;
		dty = (t_max.y - t_min.y)/gr->ny;
		dtz = (t_max.z - t_min.z)/gr->nz;
		if (rd.x > 0.0f) {
			tx_next = t_min.x + (ix + 1) * dtx;
			ix_step = +1;
			ix_stop = gr->nx;
		} else {
			tx_next = t_min.x + (gr->nx - ix) * dtx;
			ix_step = -1;
			ix_stop = -1;
		}

		if (rd.y > 0.0f) {
			ty_next = t_min.y + (iy + 1) * dty;
			iy_step = +1;
			iy_stop = gr->ny;
		} else {
			ty_next = t_min.y + (gr->ny - iy) * dty;
			iy_step = -1;
			iy_stop = -1;
		}

		if (rd.z > 0.0f) {
			tz_next = t_min.z + (iz + 1) * dtz;
			iz_step = +1;
			iz_stop = gr->nz;
		} else {
			tz_next = t_min.z + (gr->nz - iz) * dtz;
			iz_step = -1;
			iz_stop = -1;
		}


		src.r = 0.0f;
		src.g = 0.0f;
		src.b = 0.0f;
		src.alpha = 0.0f;

		dst.r = 0.0f;
		dst.g = 0.0f;
		dst.b = 0.0f;
		dst.alpha = 0.0f;

		iter = 0;
		while(iter < MAX_DEPTH)
		{
			index = ix + gr->nx * iy + gr->nx * gr->ny * iz;

			src.r = data[index];
			src.g = data[index];
			src.b = data[index];
			src.alpha = data[index];
			tr = simpleTF(src.alpha);

			src.r = tr.r * tr.alpha;
			src.g = tr.g * tr.alpha;
			src.b = tr.b * tr.alpha;

			src.alpha *= tr.alpha;

			dst.r = (1.0f - dst.alpha)*src.r + dst.r;
			dst.g = (1.0f - dst.alpha)*src.g + dst.g;
			dst.b = (1.0f - dst.alpha)*src.b + dst.b;
			dst.alpha = (1.0 - dst.alpha)*src.alpha + dst.alpha;
			if(dst.alpha >= 0.95f){
				return dst;
			}
			iter ++;

			/* X-AXIS */
			if (tx_next < ty_next && tx_next < tz_next) {

				tx_next += dtx;
				ix += ix_step;

				if(ix == ix_stop) {
					dst.r *= brightness;
					dst.g *= brightness;
					dst.b *= brightness;
					return dst; /*color*/
				}
			}
			else
			{
				if (ty_next < tz_next) {

					ty_next += dty;
					iy += iy_step;

					if (iy == iy_stop) {
						dst.r *= brightness;
						dst.g *= brightness;
						dst.b *= brightness;
						return dst; /*color*/
					}
				}
				else
				{
					tz_next += dtz;
					iz += iz_step;

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

__kernel void raycast(__global float scData[], int samples, float grid_p0_x,
		float  grid_p1_x, float  grid_p0_y, float  grid_p1_y, float  grid_p0_z,
		float  grid_p1_z, int nx, int ny, int nz, float cam_lookat_x, float cam_lookat_y,
		float cam_lookat_z, float cam_eye_x, float cam_eye_y, float cam_eye_z,
		int width, int height, __global int *image, __global int *irand, __global float *urand_x,
		__global float *urand_y, __global int *m_debug){

	int index;
	float r, g, b;
	r = g = b = 0.0f;
	float rcp_samples = 1.0f / (float)samples;

	int s;
	//int i = get_global_id(0);
	//int j = get_global_id(1);

    unsigned int work_item_id = get_global_id(0);	/* the unique global id of the work item for the current pixel */
    unsigned int i = work_item_id % width;			/* x-coordinate of the pixel */
    unsigned int j = work_item_id / width;

	//index = j*width + i; // used to fuck all up here! i*height + j
	index = j*width + i;
	m_debug[index] = 255;

	for(s=0; s<samples; s++) {
		__local grid gr;
		gr.p0.x = grid_p0_x ;
		gr.p0.y = grid_p0_y;
		gr.p0.z = grid_p0_z;
		gr.p1.x = grid_p1_x ;
		gr.p1.y = grid_p1_y;
		gr.p1.z = grid_p1_z;
		gr.nx = nx;
		gr.ny = ny;
		gr.nz = nz;
		gr.total = nx*ny*nz;

		__local camera c;
		c.lookat.x = cam_lookat_x;
		c.lookat.y = cam_lookat_y;
		c.lookat.z = cam_lookat_z;
		c.eye.x = cam_eye_x;
		c.eye.y = cam_eye_y;
		c.eye.z = cam_eye_z;
		c.view.width = width;
		c.view.height = height;

		ray rr = get_primary_ray(&c,i,j,s, irand, urand_x, urand_y, width, height);

		color col = intersectGrid(&gr,&rr, scData, 1.0f);
		r += col.r;
		g += col.g;
		b += col.b;
	}

	r = r * rcp_samples;
	g = g * rcp_samples;
	b = b * rcp_samples;
	
	image[3* (index) + 0] = (char) (r * MAX_BYTE);
	image[3* (index) + 1] = (char) (g * MAX_BYTE);
	image[3* (index) + 2] = (char) (b * MAX_BYTE);
}