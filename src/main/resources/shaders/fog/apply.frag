#version 150 core

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D uColorTexture;
uniform sampler2D uDepthTexture;


/** 
 * Fog application shader
 *
 * This merges the rendered fog onto DH's rendered LODs
 */
void main()
{
    fragColor = vec4(0.0);

    // a fragment depth of "1" means the fragment wasn't drawn to,
    // only update fragments that were drawn to
    float fragmentDepth = textureLod(uDepthTexture, TexCoord, 0).r;
    if (fragmentDepth != 1)
    {
        fragColor = texture(uColorTexture, TexCoord);
    }
}
