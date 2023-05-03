#version 330 core

in vec2 TexCoord;

uniform sampler2D screenTexture;

out vec4 FragColor;

void inversion()
{
    //Inversion
    FragColor = vec4(vec3(1.0 - texture(screenTexture, TexCoord)), 1.0);
}

void greyscale(){
    //Greyscale
    FragColor = texture(screenTexture, TexCoord);
    float average = (FragColor.r + FragColor.g + FragColor.b) / 3.0;
    FragColor = vec4(average, average, average, 1.0);
}

void kernel(){
    const float offset = 1.0 / 300.0;
    vec2 offsets[9] = vec2[](
    vec2(-offset,  offset), // top-left
    vec2( 0.0f,    offset), // top-center
    vec2( offset,  offset), // top-right
    vec2(-offset,  0.0f),   // center-left
    vec2( 0.0f,    0.0f),   // center-center
    vec2( offset,  0.0f),   // center-right
    vec2(-offset, -offset), // bottom-left
    vec2( 0.0f,   -offset), // bottom-center
    vec2( offset, -offset)  // bottom-right
    );

    float kernel[9] = float[](
    -1, -1, -1,
    -1,  9, -1,
    -1, -1, -1
    );

    vec3 sampleTex[9];
    for(int i = 0; i < 9; i++)
    {
        sampleTex[i] = vec3(texture(screenTexture, TexCoord.st + offsets[i]));
    }
    vec3 col = vec3(0.0);
    for(int i = 0; i < 9; i++)
    col += sampleTex[i] * kernel[i];

    FragColor = vec4(col, 1.0);
}

void main()
{
    float depthValue = texture(screenTexture, TexCoord).r;
    FragColor = texture(screenTexture, TexCoord);
//    FragColor = vec4(vec3(depthValue), 1.0);

//    inversion()
//    greyscale();
//    kernel();
}
