#ifndef M_UTILS_H_
#define M_UTILS_H_

//#include <stdio.h>
//#include <stdlib.h>
//#include <math.h>
//#include <float.h>
//#include <string.h>

#define TRUE (0==0)
#define FALSE (0==1)

#define MAX_BYTE 255
#define MAX_DEPTH 100000

#define SCALE_RATE 100.0f

#define WIDTH 800.0
#define HEIGHT 600.0

#define PI ((float) (355/113))
#define FOV			((float) (PI/4))		/* field of view in rads (pi/4) */
#define HALF_FOV		(FOV * 0.5)
#define NRAN	1024
#define MASK	(NRAN - 1)

#define RAY_MAG 1000


typedef unsigned char uchar;

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

void printTable(float **m, int size);
ray get_primary_ray(camera *cam, int x, int y, int sample);
point get_sample_pos(int x, int y, int sample);
point jitter(int x, int y, int s);
color simpleTF(float alpha);

float * scaleData(uchar *data,size_t size);
int inside_grid(grid *gr, point * p);
color intersectGrid(grid * gr, ray * raio, float *data, float brightness);
void writePixel(camera *c, uchar *frame, uchar r, uchar g, uchar b, int i, int j);
void initImage(camera *c,uchar * frame);
void setupGrid(grid *g,point max, point min, int nx, int ny, int nz);
ray * generatePrimaryRays(camera *c);
float dist(point *o , point *d);
void setupCamera(camera *c);
void initCamera(camera *c, point eye, point lookat, int width, int height);
void printPrimaryRays(ray *r, int size);
void normalize(point *a);
void crossProduct(point *a, point *b, point *r);
float dotProduct(point *a, point *b);
uchar * loadRawFile(char *filename, size_t bytes);
void printRawFile(uchar * data, size_t bytes);
//float distance(point a, point b);
float clamp(float x, float min, float max);
float m_max(float a, float b);
float m_min(float a, float b);
void intersectBoundingBox(grid * gr, ray * raio, point *t_min, point *t_max, float *t0, float *t1);
int save_bmp(char * file_name, camera *c, uchar *frame);

#endif
