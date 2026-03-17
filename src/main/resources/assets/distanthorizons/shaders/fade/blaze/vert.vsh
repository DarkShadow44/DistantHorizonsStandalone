#version 150 core

in vec2 vPosition;
in vec4 vColor;

out vec4 fColor;
out vec2 TexCoord;

// DH vert fade test
void main()
{
    gl_Position = vec4(vPosition, 0.0, 1.0);
    fColor = vec4(vPosition, 0.0, 1.0);
    TexCoord = vPosition.xy * 0.5 + 0.5;
}