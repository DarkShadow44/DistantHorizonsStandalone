#version 150 core

in uvec4 vPosition;
out vec4 vPos;
in vec4 color;
in uint lastTimeUpdateLo;
in uint lastTimeUpdateHi;
in uvec4 quadPos;

out vec4 vertexColor;
out vec3 vertexWorldPos;
out float vertexYPos;
flat out int isSnowy;

uniform bool uWhiteWorld;

uniform mat4 uCombinedMatrix;
uniform vec3 uModelOffset;
uniform float uWorldYOffset;

uniform int uWorldSkyLight;
uniform sampler2D uLightMap;
uniform float uMircoOffset;

uniform usampler2D uLastTimeUpdateWinterSummer;
uniform usampler2D uLastTimeUpdateAny;


/** 
 * TODO in the future this and curve.vert should be merged together to prevent inconsistencies between the two
 *
 * Vertex Shader
 * 
 * author: James Seibel
 * updated: TomTheFurry
 * updated: coolGi
 * version: 2023-6-25
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
    float my = (mirco & 4u)!=0u ? uMircoOffset : 0.0;
    my = (mirco & 8u)!=0u ? -my : my;
    float mz = (mirco & 16u)!=0u ? uMircoOffset : 0.0;
    mz = (mirco & 32u)!=0u ? -mz : mz;

    uint lights = meta & 0xFFu;

	float light2 = (mod(float(lights), 16.0)+0.5) / 16.0;
	float light = (float(lights/16u)+0.5) / 16.0;
	vertexColor = vec4(texture(uLightMap, vec2(light, light2)).xyz, 1.0);
    
    if (!uWhiteWorld)
    {
        vertexColor *= color;
    }

    gl_Position = uCombinedMatrix * vec4(vertexWorldPos + vec3(mx, 0, mz), 1.0);

    // Snow logic

    // bit 1 = isSnow
    // bit 2 = isPotentialSnow
    // bit 3 = isPermaSnow
    // bit 4 = isPermaThaw
    uint snowFlags = quadPos.y;
    bool isSnow = (snowFlags & 0x1u) != 0u;
    bool isPotentialSnow = (snowFlags & 0x2u) != 0u;
    bool isPermaSnow = (snowFlags & 0x4u) != 0u;
    bool isPermaThaw = (snowFlags & 0x8u) != 0u;

    vec2 uvLastTimeUpdate = vec2(quadPos.x + 0.5, quadPos.z + 0.5);
    uvec4 vecLastTimeUpdateWinterSummer = texture(uLastTimeUpdateWinterSummer, uvLastTimeUpdate);
    uvec4 vecLastTimeUpdateAny = texture(uLastTimeUpdateAny, uvLastTimeUpdate);

    uint lastSnowTickWinterLo = vecLastTimeUpdateWinterSummer.x;
    uint lastSnowTickWinterHi = vecLastTimeUpdateWinterSummer.y;
    uint lastThawTickSummerLo = vecLastTimeUpdateWinterSummer.z;
    uint lastThawTickSummerHi = vecLastTimeUpdateWinterSummer.a;
    uint lastSnowTickAnyLo = vecLastTimeUpdateAny.x;
    uint lastSnowTickAnyHi = vecLastTimeUpdateAny.y;
    uint lastThawTickAnyLo = vecLastTimeUpdateAny.z;
    uint lastThawTickAnyHi = vecLastTimeUpdateAny.a;
    
    isSnowy = isSnow ? 1 : 0;
    if (isPermaThaw) {
        if (lastThawTickAnyLo > lastTimeUpdateLo) {
            isSnowy = 0;
        }
    } else if (isPermaSnow) {
        if (lastSnowTickAnyLo > lastTimeUpdateLo) {
            isSnowy = 1;
        }
    } else if (lastThawTickSummerLo > lastSnowTickWinterLo) {
        if (lastThawTickSummerLo > lastTimeUpdateLo) {
            isSnowy = 0;
        }
    } else {
        if (lastSnowTickWinterLo > lastTimeUpdateLo) {
            isSnowy = 1;
        }
    }
}
