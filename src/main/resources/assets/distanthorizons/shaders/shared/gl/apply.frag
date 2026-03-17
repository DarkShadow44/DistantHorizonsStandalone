#version 150 core

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D gDhColorTexture;
uniform sampler2D gDhDepthTexture;


/** 
 * LOD application shader
 *
 * This merges the rendered LODs into Minecraft's texture/FBO   
 */
void main()
{
    fragColor = vec4(0.0);
    
    // a fragment depth of "1" means the fragment wasn't drawn to,
    // only update fragments that were drawn to
    float fragmentDepth = texture(gDhDepthTexture, TexCoord).r;
    if (fragmentDepth != 1)
    {
        fragColor = texture(gDhColorTexture, TexCoord);
    }
    else
    {
        // use the original MC texture if no LODs were drawn to this fragment
        discard;
    }
}

