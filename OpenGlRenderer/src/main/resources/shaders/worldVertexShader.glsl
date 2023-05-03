#version 330 core

in vec3 aPos;
in vec2 aTexCoord;
in vec3 aNormal;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;
uniform mat4 lightSpaceMatrix;

out vec3 FragPos;
out vec3 Normal;
out vec2 TexCoords;
out vec4 FragPosLightSpace;
flat out vec3 VertexColor;

vec3 positionToColor(float value) {
    vec3 color = vec3(0.0);
    color.x = fract(sin(value * 17.0) * 71.0);
    color.y = fract(sin(value * 13.0) * 67.0);
    color.z = fract(sin(value * 19.0) * 73.0);
    return color;
}


void main()
{
    gl_Position = projection * view * model * vec4(aPos, 1.0);
    FragPos = vec3(model * vec4(aPos, 1.0));
    Normal = mat3(transpose(inverse(model))) * aNormal;
    TexCoords = aTexCoord;
    FragPosLightSpace = lightSpaceMatrix * vec4(FragPos, 1.0);

    int index = gl_VertexID / 3;
    VertexColor = floor(positionToColor(float(index)) * 8.0) / 8.0;
}
