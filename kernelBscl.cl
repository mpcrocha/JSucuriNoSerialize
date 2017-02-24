float cdf(float x) {
  
  // constants cdf
  float a1 =  0.254829592;
  float a2 = -0.284496736;
  float a3 =  1.421413741;
  float a4 = -1.453152027;
  float a5 =  1.061405429;
  float p  =  0.3275911;
 
  // sign x
  int sign = 1;
  sign = (sign * (x>=0)) + ((-sign)*(x<0));
  
  x = fabs(x)/sqrt(2.0);
 
  // A&S formula 7.1.26
  float t = 1.0/(1.0 + p*x);
  float y = 1.0 - (((((a5*t + a4)*t) + a3)*t + a2)*t + a1)*t*exp(-x*x);
 
  float cdf = 0.5*(1.0 + sign*y);
  
  return cdf;
  
}

/*
# Price an option using the Black-Scholes model.
#       s: initial stock price
#       k: strike price
#       t: expiration time
#       v: volatility
#       rf: risk-free rate
#       div: dividend
#       cp: +1/-1 for call/put
*/


__kernel void bscl(int dim, __global const float *a_g, 
__global const float *s_g, __global const float *t_g, __global float *res_g) {
  
  int gid = get_global_id(0);
  //int dim = dimension[0];
  
  float s = s_g[gid];
  printf("s kernel:%f\n", s);
  float k = a_g[(gid * dim) + 0];
  float rf = a_g[(gid * dim) + 1];
  float v = a_g[(gid * dim) + 2];
  float t = t_g[gid];
  //printf("t kernel:%f\n", t);
  float cp = a_g[(gid * dim) + 3];
  float div = a_g[(gid * dim) + 4];
  
  
  
  
  float optprice  = 0;
  
  float d1 = (log(s/k)+((rf-div)+(0.5*(pow(v,2))))*t)/(v*sqrt(t));
  float d2 = d1 - v*sqrt(t);
  
  float cp_d1 = cp*d1;
  float cp_d2 = cp*d2;
  optprice = (cp*s*exp(-div*t)*cdf(cp_d1)) - (cp*k*exp(-rf*t)*cdf(cp_d2));
  
  
  res_g[gid] = optprice;
}

  