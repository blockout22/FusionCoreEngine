#version 330 core

struct Material {
    sampler2D diffuse;
    vec3 specular;
    float shininess;
};

struct DirLight {
    vec3 direction;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
};

struct PointLight {
    vec3 position;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float constant;
    float linear;
    float quadratic;
};

struct SpotLight {
    vec3 position;
    vec3 direction;
    vec3 ambient;
    vec3 diffuse;
    vec3 specular;
    float constant;
    float linear;
    float quadratic;
    float cutOff;
    float outerCutOff;
};

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoord;

uniform Material material;
uniform DirLight dirLight;
uniform PointLight pointLights[4];
uniform SpotLight spotLights[4];
uniform int numPointLights;
uniform int numSpotLights;
uniform vec3 viewPos;

uniform sampler2D depthMap; // The depth texture from the depth pass
uniform mat4 lightSpaceMatrix; // The light's view-projection matrix

out vec4 FragColor;

// Calculate the diffuse lighting component using the Lambertian model
vec3 calculateDiffuse(DirLight light, vec3 norm) {
    float diff = max(dot(norm, -light.direction), 0.0);
    vec3 diffuse = light.diffuse * diff;
    return diffuse;
}

vec3 calculateDiffuse(PointLight light, vec3 norm, vec3 fragPos) {
    vec3 lightDir = normalize(light.position - fragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = light.diffuse * diff;
    return diffuse;
}

vec3 calculateDiffuse(SpotLight light, vec3 norm, vec3 fragPos) {
    vec3 lightDir = normalize(light.position - fragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = light.diffuse * diff;
    return diffuse;
}

// Calculate the specular lighting component using the Blinn-Phong model
vec3 calculateSpecular(DirLight light, vec3 norm, vec3 viewDir) {
    vec3 halfwayDir = normalize(light.direction + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), material.shininess);
    vec3 specular = light.specular * spec;
    return specular;
}

vec3 calculateSpecular(PointLight light, vec3 norm, vec3 fragPos, vec3 viewDir) {
    vec3 lightDir = normalize(light.position - fragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), material.shininess);
    vec3 specular = light.specular * spec;
    return specular;
}

vec3 calculateSpecular(SpotLight light, vec3 norm, vec3 fragPos, vec3 viewDir) {
    vec3 lightDir = normalize(light.position - fragPos);
    vec3 halfwayDir = normalize(lightDir + viewDir);
    float spec = pow(max(dot(norm, halfwayDir), 0.0), material.shininess);
    vec3 specular = light.specular * spec;
    return specular;
}

// Calculate the attenuation factor for a point light
float calculateAttenuation(PointLight light, vec3 fragPos) {
    float distance = length(light.position - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));
    return attenuation;
}

// Calculate the attenuation factor for a spot light
float calculateAttenuation(SpotLight light, vec3 fragPos) {
    float distance = length(light.position - fragPos);
    float attenuation = 1.0 / (light.constant + light.linear * distance + light.quadratic * (distance * distance));
    // Calculate the spotlight intensity based on the angle between the light direction and the spotlight cone
    vec3 lightDir = normalize(light.position - fragPos);
    float cosAngle = dot(lightDir, normalize(-light.direction));
    if (cosAngle < light.cutOff) {
        attenuation = 0.0;
    } else if (cosAngle < light.outerCutOff) {
        float factor = smoothstep(light.cutOff, light.outerCutOff, cosAngle);
        attenuation *= factor;
    }
    return attenuation;
}

float ShadowCalculation(vec3 fragPos)
{
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(fragPos, 1.0);
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    vec2 uvCoords = projCoords.xy * 0.5 + 0.5;
    float depth = texture(depthMap, uvCoords).r;
    float currentDepth = projCoords.z;
    float bias = max(0.05 * (1.0 - dot(Normal, vec3(0.0, 1.0, 0.0))), 0.005);
    float shadow = currentDepth - bias > depth ? 1.0 : 0.0;

    return shadow;
}

void test(){
    vec3 color = texture(material.diffuse, TexCoord).rgb;
    // ambient
    vec3 ambient = 0.05 * color;
    // diffuse
    vec3 lightDir = normalize(vec3(0, 3, 0) - FragPos);
    vec3 normal = normalize(Normal);
    float diff = max(dot(vec3(0, -1, 0), normal), 0.0);
    vec3 diffuse = diff * color;
    // specular
    vec3 viewDir = normalize(viewPos - FragPos);
    vec3 reflectDir = reflect(-lightDir, normal);
    float spec = 0.0;


        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(normal, halfwayDir), 0.0), 32.0);

//        vec3 reflectDir = reflect(-lightDir, normal);
//        spec = pow(max(dot(viewDir, reflectDir), 0.0), 8.0);

    vec3 specular = vec3(0.3) * spec; // assuming bright white light color
    FragColor = vec4(ambient + diffuse + specular, 1.0);
}


void main() {
    // Normalize the input normal vector
    vec3 norm = normalize(Normal);

    // Calculate the diffuse and specular lighting components for each light
    vec3 diffuse = vec3(0.0);
    vec3 specular = vec3(0.0);
    for (int i = 0; i < numPointLights; i++) {
        diffuse += calculateDiffuse(pointLights[i], norm, FragPos);
        specular += calculateSpecular(pointLights[i], norm, FragPos, normalize(viewPos - FragPos));
    }
    for (int i = 0; i < numSpotLights; i++) {
        diffuse += calculateDiffuse(spotLights[i], norm, FragPos);
        specular += calculateSpecular(spotLights[i], norm, FragPos, normalize(viewPos - FragPos));
    }
    diffuse += calculateDiffuse(dirLight, norm);
    specular += calculateSpecular(dirLight, norm, normalize(viewPos - FragPos));

    // Sample the object texture
    vec3 objectColor = texture(material.diffuse, TexCoord).rgb;

    float shadow = ShadowCalculation(FragPos);

    // Calculate the final color based on the material properties and lighting components
    vec3 lighting = (diffuse + specular) * objectColor * (1.0 - shadow);
    vec3 ambient = dirLight.ambient + pointLights[0].ambient;
    vec3 finalColor = ambient + lighting;

    // Set the output color
    FragColor = vec4(finalColor, 1.0);

//    test();
}

