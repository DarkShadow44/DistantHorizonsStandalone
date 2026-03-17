#version 150 core

layout (std140) uniform uniformBlock
{
    mat4 uTransform;
    vec4 uColor;
};

out vec4 fragColor;

void main()
{
    fragColor = uColor;
}