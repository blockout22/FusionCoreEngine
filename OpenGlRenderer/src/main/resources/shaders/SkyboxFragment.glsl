#version 330 core
out vec4 FragColor;

in vec3 TexCoords;

uniform vec3 lightDirection; // Pass the direction of the light to the shader

void main()
{
    // Normalize the texture coordinates and the light direction
    vec3 texCoordsNormalized = normalize(TexCoords);
    vec3 lightDirectionNormalized = normalize(lightDirection);

    // Calculate the angle between the texture coordinates and the light direction
    float angle = acos(dot(texCoordsNormalized, lightDirectionNormalized));

    // Calculate the height of the sun in the sky based on the light direction
    float sunHeight = lightDirectionNormalized.y;

    // Define the colors of the sun and the sky based on the sun height
    vec3 sunColorHigh = vec3(1.0, 0.89, 0.7); // Slightly orange-yellow
    vec3 skyColorHigh = vec3(0.5, 0.7, 1.0); // Deep blue

    vec3 sunColorLow = vec3(1.0, 0.5, 0.0); // Orange-red
    vec3 skyColorLow = vec3(0.0, 0.0, 0.0); // Very dark black

    vec3 sunColorMid = vec3(1.0, 0.65, 0.35); // Blend between high and low
    vec3 skyColorMid = vec3(0.7, 0.8, 1.0); // Blend between high and low

    // Interpolate between the color sets based on the sun height
    vec3 sunColor = mix(sunColorLow, sunColorMid, smoothstep(-1.0, 0.0, sunHeight));
    sunColor = mix(sunColor, sunColorHigh, smoothstep(0.0, 1.0, sunHeight));

    vec3 skyColor = mix(skyColorLow, skyColorMid, smoothstep(-1.0, 0.0, sunHeight));
    skyColor = mix(skyColor, skyColorHigh, smoothstep(0.0, 1.0, sunHeight));

    // Define the radius of the sun and the softness of its edge
    float sunRadius = 0.02;
    float sunEdgeSoftness = 0.01;

    // Calculate the factor for the sun based on the angle
    float sunFactor = smoothstep(sunRadius, sunRadius + sunEdgeSoftness, angle);

    // Calculate the final color by interpolating between the sun color and the sky color based on the sun factor
    vec3 finalColor = mix(sunColor, skyColor, sunFactor);

    FragColor = vec4(finalColor, 1.0);
}
