#version 330 core

in vec2 aPos;
in vec2 aTexCoord;

out vec2 TexCoord;

void main()
{
    gl_Position = vec4(aPos.x, aPos.y,  0.0, 1.0);
    TexCoord = aTexCoord;
}
