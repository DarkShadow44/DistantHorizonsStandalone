#version 150 core

in vec2 TexCoord;

out vec4 fragColor;

// inverted model view matrix and projection matrix
uniform mat4 uDhInvMvmProj;

uniform sampler2D uDhDepthTexture;
uniform sampler2D uMcColorTexture;
uniform sampler2D uDhColorTexture;

uniform float uStartFadeBlockDistance;
uniform float uEndFadeBlockDistance;



vec3 calcViewPosition(float fragmentDepth, mat4 invMvmProj) 
{
    // normalized device coordinates
    vec4 ndc = vec4(TexCoord.xy, fragmentDepth, 1.0);
    ndc.xyz = ndc.xyz * 2.0 - 1.0;

    vec4 eyeCoord = invMvmProj * ndc;
    return eyeCoord.xyz / eyeCoord.w;
}

/**
 * Used to fade out vanilla chunks so the transition
 * between DH and vanilla is smoother.
 */
void main() 
{
    // includes both the vanilla chunks as well as DH
    vec4 combinedMcDhColor = texture(uMcColorTexture, TexCoord);
    // just the DH render pass
    vec4 dhColor = texture(uDhColorTexture, TexCoord);
    
    
    
    // the DH texture will have white if nothing was written to that pixel.
    // TODO replace with a depth texture check, this feels janky
    if (dhColor == vec4(1))
    {
        // if not done vanilla clouds will render incorrectly at night
        dhColor = combinedMcDhColor;
    }
    
    
    float dhFragmentDepth = texture(uDhDepthTexture, TexCoord).r;
    vec3 dhVertexWorldPos = calcViewPosition(dhFragmentDepth, uDhInvMvmProj);
    float dhFragmentDistance = length(dhVertexWorldPos.xzy);
    
    
    float startFade = uEndFadeBlockDistance;
    float endFade = uStartFadeBlockDistance;
    
    // Smoothly transition between combinedMcDhColor and uDhColorTexture
    // as the depth increases from the camera
    float fadeStep = smoothstep(startFade, endFade, dhFragmentDistance);
    fragColor = mix(combinedMcDhColor, dhColor, fadeStep);
    fragColor.a = 1.0; // TODO is setting the alpha needed?
    
}

