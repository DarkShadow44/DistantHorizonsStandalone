#version 150 core

in uvec4 vPosition;
out vec4 vPos;
in vec4 color;

out vec4 vertexColor;
out vec3 vertexWorldPos;
out float vertexYPos;

uniform bool uIsWhiteWorld;

uniform mat4 uCombinedMatrix;
uniform vec3 uModelOffset;
uniform float uWorldYOffset;

uniform sampler2D uLightMap;
uniform float uMircoOffset;

uniform float uEarthRadius;

/** 
 * Vertex Shader
 * 
 * author: James Seibel
 * author: TomTheFurry
 * author: stduhpf
 * updated: coolGi
 *
 * version: 2025-12-22
 */
void main()
{
    vPos = vPosition; // This is so it can be passed to the fragment shader
    
    vertexWorldPos = vPosition.xyz + uModelOffset;
    
    vertexYPos = vPosition.y + uWorldYOffset;
    
    uint meta = vPosition.a;
    
    uint mirco = (meta & 0xFF00u) >> 8u; // mirco offset which is a xyz 2bit value
    // 0b00 = no offset
    // 0b01 = positive offset
    // 0b11 = negative offset
    // format is: 0b00zzyyxx
    float mx = (mirco & 1u)!=0u ? uMircoOffset : 0.0;
    mx = (mirco & 2u)!=0u ? -mx : mx;
    //float my = (mirco & 4u)!=0u ? uMircoOffset : 0.0;
    //my = (mirco & 8u)!=0u ? -my : my;
    float mz = (mirco & 16u)!=0u ? uMircoOffset : 0.0;
    mz = (mirco & 32u)!=0u ? -mz : mz;
    
    vertexWorldPos.x += mx;
    //vertexWorldPos.y += my;
    vertexWorldPos.z += mz;
    
    // apply the earth curvature if needed
    if (uEarthRadius < -1.0f || uEarthRadius > 1.0f)
    {
        // vertex transformation logic - stduhpf
        float localRadius = uEarthRadius + vertexYPos;
        float phi = length(vertexWorldPos.xz) / localRadius;
        vertexWorldPos.y += (cos(phi) - 1.0) * localRadius;
        vertexWorldPos.xz = vertexWorldPos.xz * sin(phi) / phi;
    }
    
    uint lights = meta & 0xFFu;
    float skyLight = (float(lights/16u)+0.5) / 16.0;
    float blockLight = (mod(float(lights), 16.0)+0.5) / 16.0;
    vertexColor = vec4(texture(uLightMap, vec2(skyLight, blockLight)).xyz, 1.0);
    
    if (!uIsWhiteWorld)
    {
        vertexColor *= color;
    }
    
    gl_Position = uCombinedMatrix * vec4(vertexWorldPos, 1.0);
}
