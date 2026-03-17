#version 150 core

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D uFadeColorTextureUniform;



void main()
{
    fragColor = texture(uFadeColorTextureUniform, TexCoord);
}
