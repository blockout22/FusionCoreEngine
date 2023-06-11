#version 330 core

struct Material {
    sampler2D diffuse;
    sampler2D specular;
    float shininess;
};

struct DirLight{
    vec3 direction;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
};
uniform DirLight dirLight;

struct PointLight {
    vec3 position;

    vec3 ambient;
    vec3 diffuse;
    vec3 specular;

    float constant;
    float linear;
    float quadratic;
};
uniform PointLight light;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;
flat in vec3 VertexColor;

uniform Material material;
uniform vec3 viewPos;
uniform sampler2D shadowMap;
uniform float gamma;
uniform vec3 objectColor = vec3(1, 1, 1);
uniform int unlit;

out vec4 FragColor;

void setGamma(float value){
    FragColor.rgb = pow(FragColor.rgb, vec3(1.0 / value));
}

vec3 getTriangleColor() {
    vec3 barycentricCoords = vec3(gl_FragCoord.x, gl_FragCoord.y, 1.0 - gl_FragCoord.x - gl_FragCoord.y);
    return (VertexColor * barycentricCoords.x + VertexColor * barycentricCoords.y + VertexColor * barycentricCoords.z) / 3.0;
}

//float ShadowCalc(vec4 fragPosLightSpace, vec3 lightDir){
//    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
//    projCoords = projCoords * 0.5 + 0.5;
//    float closestDepth = texture(shadowMap, projCoords.xy).r;
//    float currentDepth = projCoords.z;
//
//    float bias = max(0.05 * (1.0 - dot(normalize(Normal), normalize(-lightDir))), 0.005);
////    float bias = .01;
//    float shadow = 0.0; //currentDepth - bias > closestDepth  ? 1.0 : 0.0;
//    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
//    for(int x = -1; x <= 1; ++x)
//    {
//        for(int y = -1; y <= 1; ++y)
//        {
//            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
//            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
//        }
//    }
//    shadow /= 9.0;
//
//    if(projCoords.z > 1.0){
//        shadow = 0.0;
//    }
//
//    // Ensure the shadow value is clamped between 0 and 1.
//    shadow = clamp(shadow, 0.0, 1.0);
//
//    return shadow;
//}

//float ShadowCalculation(vec4 fragPosLightSpace, vec3 lightDir){
//    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
//    // transform to [0,1] range
//    projCoords = projCoords * 0.5 + 0.5;
//    // get closest depth value from light's perspective (using [0,1] range fragPosLight as coords)
//    float closestDepth = texture(shadowMap, projCoords.xy).r;
//    // get depth of current fragment from light's perspective
//    float currentDepth = projCoords.z;
//    // Apply bias
//    // Slope-scaled depth bias
////    float bias = 0.001 * tan(acos(max(dot(normalize(Normal), normalize(-lightDir)), 0.1)));
////    bias = clamp(bias, 0.005, 0.01);
//    float bias = max(0.0005 * (1.0 - dot(Normal, lightDir)), 0.0005);
//    // check whether current frag pos is in shadow
//    float shadow = currentDepth - bias > closestDepth  ? 1.0 : 0.0;
//    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
//    for(int x = -1; x <= 1; ++x)
//    {
//        for(int y = -1; y <= 1; ++y)
//        {
//            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
//            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
//        }
//    }
//    shadow /= 9.0;
//
//    if(projCoords.z > 1.0){
//        shadow = 0.0;
//    }
//
//    return shadow;
//}

float shadowC(float dotLightNormal){
    vec3 pos = FragPosLightSpace.xyz * 0.5 + 0.5;
    if(pos.z > 1.0){
        pos.z = 1.0;
    }
    float depth = texture(shadowMap, pos.xy).r;
    //good for 16384x16384 res
    float bias = max(0.0001 * (1.0 - dotLightNormal), 0.00001);
    //good for 1080x1080 res
//    float bias = max(0.005 * (1.0 - dotLightNormal), 0.0005);
    float shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x){
        for(int y = -1; y <= 1; ++y){
            float depth = texture(shadowMap, pos.xy + vec2(x, y) * texelSize).r;
            shadow += (depth + bias) < pos.z ? 0.4 : 1.0;
        }
    }
    return shadow / 9.0;
}

void main(){
    vec3 color = texture(material.diffuse, TexCoords).rgb;
    vec3 normal = normalize(Normal);
    vec3 lightColor = vec3(0.3);

    vec3 ambient = dirLight.ambient * color;//0.3 * color;

    float distanceFromCenter = 100;
    vec3 lightPos = normalize(dirLight.direction) * distanceFromCenter + vec3(0, 0, 0);
    vec3 lightDir = normalize(lightPos - FragPos);
    float dotLightNormal = dot(lightDir, normal);
    float diff = max(dotLightNormal, 0.0);
//    vec3 diffuse = diff * lightColor;
    vec3 diffuse = dirLight.diffuse * diff * color;

    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(normal, halfwayDir), 0.0), material.shininess);
//    vec3 specular = spec * lightColor;
    vec3 specular = dirLight.specular * spec * vec3(texture(material.specular, TexCoords));

    float shadow = shadowC(dotLightNormal);
    vec3 lighting = ((unlit == 1 ? 1 : shadow) * (diffuse + specular) + ambient) * color;
    FragColor = vec4(lighting, 1.0);
}

//void main(){
//
//    vec3 color = texture(material.diffuse, TexCoords).rgb * objectColor;
//    vec3 norm = normalize(Normal);
//
//    //ambient
//    vec3 ambient = dirLight.ambient * vec3(texture(material.diffuse, TexCoords));
//
//    //diffuse
//    vec3 lightDir = normalize(-dirLight.direction);
//    float diff = max(dot(norm, lightDir), 0.0);
//    vec3 diffuse = dirLight.diffuse * (diff * vec3(texture(material.diffuse, TexCoords)));
//
//    //specular
//    vec3 viewDir = normalize(viewPos - FragPos);
//    vec3 reflectDir = reflect(-lightDir, norm);
//    float spec = pow(max(dot(viewDir, reflectDir), 0.0), material.shininess);
//    vec3 specular = dirLight.specular * spec * vec3(texture(material.specular, TexCoords));
//
//
//    //attenuation
////    float distance = length(light.position - FragPos);
////    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));
////    ambient *= attenuation;
////    diffuse *= attenuation;
////    specular *= attenuation;
//
//    //calculate shadow
//    float shadow;// = ShadowCalculation(FragPosLightSpace, -lightDir);
//    shadow = ShadowCalc(FragPosLightSpace, -lightDir);
//    vec3 lighting = ((unlit == 1 ? 1 : 1.0 - shadow) * (ambient + diffuse + specular)) * color;
//    //    vec3 lighting = (ambient + (1.0 - shadow) * (diffuse + specular)) * getTriangleColor();
//
////    vec3 result = ambient + diffuse + specular * color;
//    FragColor = vec4(lighting, 1.0);
//
////    float dotProd = dot(viewDir, norm);
////    if(dotProd > 0.0){
////        FragColor = vec4(0.0, 0.0, 1.0, 1.0);
////    }else{
////        FragColor = vec4(1.0, 0.0, 0.0, 1.0);
////    }
//
//    //a way to view normals of the objects
////    FragColor = vec4(normalize(Normal) * 0.5 + 0.5, 1.0);
//
//    //show only diffuse and ambient of models
////    FragColor = vec4(color * vec3(texture(material.diffuse, TexCoords)), 1.0);
//
//    setGamma(gamma);
//}