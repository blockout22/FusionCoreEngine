#version 330 core

out vec4 FragColor;

layout (location = 0) out float FragDepth;

float near = 0.1;
float far = 150;

void main(){

//    FragDepth = gl_FragCoord.z;

    FragColor = vec4(vec3(gl_FragCoord.z), 1.0);

//    float depth = gl_FragCoord.z;
//    float ndc = depth * 2.0 - 1.0;
//
//    float linearDepth = ((2.0 * near * far) / (far + near - ndc * (far - near))) / far;
//    FragColor = vec4(vec3(linearDepth), 1.0);

//    float depth = gl_FragCoord.z / gl_FragCoord.w; // Get the depth value
//    float linearDepth = (far - near) / (far + near - depth * (far - near)); // Calculate the linear depth
//    float depthColor = linearDepth / far; // Normalize the depth value
//    FragColor = vec4(vec3(depthColor), 1.0); // Output the color

    //
//    float ld = near * far / (far + near - depth * (far - near));
//    float scaledDepth = ld / far;
//    out_Color = vec4(vec3(scaledDepth), 1.0);
}