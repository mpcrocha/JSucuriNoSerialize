typedef struct Sphere{
	float radius;
	float3 pos;
	float3 color;
	float3 emission;
} Sphere;

__kernel void test(Sphere sphere)
{
   float3 pos = sphere.pos;
}