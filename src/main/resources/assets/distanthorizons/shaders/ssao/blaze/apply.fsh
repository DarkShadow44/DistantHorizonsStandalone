#version 150 core

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D uSourceColorTexture;
uniform sampler2D uSourceDepthTexture;

layout (std140) uniform applyFragUniformBlock
{
    vec2 uViewSize;
    int uBlurRadius;
    float uNearClipPlane; // in blocks
    float uFarClipPlane; // in blocks
};


float linearizeDepth(const in float depth) { return (uNearClipPlane * uFarClipPlane) / (depth * (uNearClipPlane - uFarClipPlane) + uFarClipPlane); }

float Gaussian(const in float sigma, const in float x) { return exp(-(x*x) / (2.0 * (sigma*sigma))); }

float BilateralGaussianBlur(const in vec2 texcoord, const in float linearDepth, const in float g_sigmaV) 
{
    float g_sigmaX = 1.6;
    float g_sigmaY = 1.6;

    int radius = clamp(uBlurRadius, 1, 3);
    
    vec2 pixelSize = 1.0 / uViewSize;

    float accum = 0.0;
    float total = 0.0;
    for (int iy = -radius; iy <= radius; iy++) 
    {
        float fy = Gaussian(g_sigmaY, iy);

        for (int ix = -radius; ix <= radius; ix++) 
        {
            float fx = Gaussian(g_sigmaX, ix);

            vec2 sampleTexCoord = texcoord + ivec2(ix, iy) * pixelSize;
            
            float sampleValue = textureLod(uSourceColorTexture, sampleTexCoord, 0).r;
            
            float sampleDepth = textureLod(uSourceDepthTexture, sampleTexCoord, 0).r;
            float sampleLinearDepth = linearizeDepth(sampleDepth);

            float depthDiff = abs(sampleLinearDepth - linearDepth);
            float fv = Gaussian(g_sigmaV, depthDiff);

            float weight = fx*fy*fv;
            accum += weight * sampleValue;
            total += weight;
        }
    }

    if (total <= 1.e-4)
    {
        return 1.0;
    }
    return accum / total;
}


void main()
{
    fragColor = vec4(1.0);
    
    float fragmentDepth = textureLod(uSourceDepthTexture, TexCoord, 0).r;

    // a fragment depth of "1" means the fragment wasn't drawn to,
    // we only want to apply SSAO to LODs, not to the sky outside the LODs
    if (fragmentDepth < 1) 
    {
        if (uBlurRadius > 0) 
        {
            float fragmentDepthLinear = linearizeDepth(fragmentDepth);
            fragColor.a = BilateralGaussianBlur(TexCoord, fragmentDepthLinear, 1.6);
        }
        else 
        {
            fragColor.a = texelFetch(uSourceColorTexture, ivec2(gl_FragCoord.xy), 0).r;
        }
    }
}
