#version 150 core

in vec2 TexCoord;

out vec4 fragColor;

uniform sampler2D uCopyTexture;

// DH copy frag
void main()
{
    fragColor = texture(uCopyTexture, TexCoord);
}