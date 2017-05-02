const sampler_t sampler = CLK_ADDRESS_CLAMP_TO_EDGE |
 CLK_FILTER_NEAREST;
__kernel void sobel_grayscale(__global float* src, __global float*  dst)
{

   /* the unique global id of the work item for the current pixel */
   int gid = (int)get_global_id(0);
   //if(gid==0)
      //printf("%f\n: ",src[0]);
   int width = 570;
   int height = 881;
   unsigned int x = gid % width; /* x-coordinate of the pixel */
   unsigned int y = gid / width; /* y-coordinate of the pixel */

   if (x >= width || y >= height)
      return;

   /*
   float4 p00 = read_imagef(src, sampler, (int2)(x - 1, y - 1));
   float4 p10 = read_imagef(src, sampler, (int2)(x, y - 1));
   float4 p20 = read_imagef(src, sampler, (int2)(x + 1, y - 1));
   float4 p01 = read_imagef(src, sampler, (int2)(x - 1, y));
   float4 p21 = read_imagef(src, sampler, (int2)(x + 1, y));
   float4 p02 = read_imagef(src, sampler, (int2)(x - 1, y + 1));
   float4 p12 = read_imagef(src, sampler, (int2)(x, y + 1));
   float4 p22 = read_imagef(src, sampler, (int2)(x + 1, y + 1));
   */

   // 4 is because number of floats of each pixel
   int p00_index_start = (gid +2) * 4;
   if(gid==4)
     printf("%d\n: ",p00_index_start);
   float4 p00 = (float4)(src[p00_index_start], src[p00_index_start +1], src[p00_index_start + 2],
                       src[p00_index_start + 3]);

   int p10_index_start = (gid + 3) * 4;
   float4 p10 = (float4)(src[p10_index_start], src[p10_index_start +1], src[p10_index_start + 2],
                       src[p10_index_start + 3]);

   int p20_index_start = (gid + 4) * 4;
   float4 p20 = (float4)(src[p20_index_start], src[p20_index_start +1], src[p20_index_start + 2],
                       src[p20_index_start + 3]);

   int p01_index_start = (gid -1) * 4;
   float4 p01 = (float4)(src[p01_index_start], src[p01_index_start +1], src[p01_index_start + 2],
                       src[p01_index_start + 3]);

   int p21_index_start = (gid + 1) * 4;
   float4 p21 = (float4)(src[p21_index_start], src[p21_index_start +1], src[p21_index_start + 2],
                       src[p21_index_start + 3]);

   int p02_index_start = (gid - 4) * 4;
   float4 p02 = (float4)(src[p02_index_start], src[p02_index_start +1], src[p02_index_start + 2],
                       src[p02_index_start + 3]);

   int p12_index_start = (gid - 3) * 4;
   float4 p12 = (float4)(src[p12_index_start], src[p12_index_start +1], src[p12_index_start + 2],
                       src[p12_index_start + 3]);

   int p22_index_start = (gid - 2) * 4;
   float4 p22 = (float4)(src[p22_index_start], src[p22_index_start +1], src[p22_index_start + 2],
                       src[p22_index_start + 3]);

   if(gid==4){
        printf("%f %f %f\n: ", p02.x, p12.x, p22.x);
        printf("%f # %f\n:  ", p01.x, p21.x);
        printf("%f %f %f\n: ", p00.x, p10.x, p20.x);
   }

   float3 gx = -p00.xyz + p20.xyz +
   2.0f * (p21.xyz - p01.xyz)
   -p02.xyz + p22.xyz;
   float3 gy = -p00.xyz - p20.xyz +
   2.0f * (p12.xyz - p10.xyz) +
   p02.xyz + p22.xyz;
   float gs_x = 0.3333f * (gx.x + gx.y + gx.z);
   float gs_y = 0.3333f * (gy.x + gy.y + gy.z);
   float g = native_sqrt(gs_x * gs_x + gs_y * gs_y);
   //write_imagef(dst, (int2)(x, y), (float4)(g, g, g, 1.0f));

   /*
   dst[(gid * 4) + 0] = g;
   dst[(gid * 4) + 1] = g;
   dst[(gid * 4) + 2] = g;
   dst[(gid * 4) + 3] = 1.0f;
   */

   dst[(gid * 4) + 0] = src[(gid * 4) + 0];
   dst[(gid * 4) + 1] = src[(gid * 4) + 0];
   dst[(gid * 4) + 2] = src[(gid * 4) + 0];
   dst[(gid * 4) + 3] = 1.0f;

}