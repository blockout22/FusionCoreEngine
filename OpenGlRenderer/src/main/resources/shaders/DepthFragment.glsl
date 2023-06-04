#version 330 core

out vec4 FragColor;

float near = 0.1;
float far = 500;

float LinearizeDepth(float depth)
{
    float z = depth * 2.0 - 1.0; // back to NDC
//    return (2.0 * near * far) / (far + near - z * (far - near));
    return (2.0 * near * far) / (far + near - depth * (far - near));
}

void main(){

//    FragDepth = gl_FragCoord.z;
//    FragColor = vec4(vec3(gl_FragCoord.z), 1.0);
    float val = gl_FragCoord.z;
    if(val > 1){
        val = 0.5;
    }else if(val > 0){
        val = 1;
    }else{
        val = 0;
    }
    FragColor.r = val;
//    float z = gl_FragCoord.z;
//    float w = gl_FragCoord.w;
//    w = z / (1.0 - z);
//    float linearDepth = z / w;
//    FragColor.r = linearDepth;
//    FragColor = vec4(vec3(LinearizeDepth(gl_FragCoord.z)), 1.0);

    // Convert depth value from [0,1] range to linear depth.
//    float linearDepth = LinearizeDepth(gl_FragCoord.z);
    // Normalize linear depth to [0,1] for visualization.
//    linearDepth /= far;
//    FragColor = vec4(vec3(linearDepth), 1.0);

//    float depth = LinearizeDepth(gl_FragCoord.z) / far; // divide by far for demonstration
//    FragColor = vec4(vec3(depth), 1.0);
}