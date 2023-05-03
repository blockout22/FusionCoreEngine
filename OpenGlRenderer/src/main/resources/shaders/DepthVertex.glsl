#version 330 core

in vec3 aPos;

uniform mat4 model;
uniform mat4 projection;
uniform mat4 view;

uniform mat4 lightSpaceMatrix;

void main(void){

//    vec4 worldPosition = model * vec4(aPos,1.0);
//    vec4 positionRelativeToCam = view * worldPosition;
//    gl_Position = projection * positionRelativeToCam;

    gl_Position = lightSpaceMatrix * model * vec4(aPos, 1.0);
}
