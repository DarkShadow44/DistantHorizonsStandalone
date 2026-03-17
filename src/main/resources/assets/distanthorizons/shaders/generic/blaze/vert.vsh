#version 330 core

//layout (location = 1) in vec4 aColor; // RGBA_FLOAT_COLOR 
//layout (location = 2) in vec3 aScale; // VEC3_SCALE
//layout (location = 3) in ivec3 aTranslateChunk; // IVEC3_SCALE
//layout (location = 4) in vec3 aTranslateSubChunk; // VEC3_SCALE
//layout (location = 5) in int aMaterial; // IRIS_MATERIAL

//uniform sampler2D /*vec4*/ uColorMap; 
//uniform sampler2D /*vec3*/ uScaleMap;
//uniform sampler2D /*int*/ uTranslateChunkXMap;
//uniform sampler2D /*int*/ uTranslateChunkYMap;
//uniform sampler2D /*int*/ uTranslateChunkZMap;
//uniform sampler2D /*vec3*/ uTranslateSubChunkMap;
//uniform sampler2D /*int*/ uMaterialMap;
//
//in vec3 vPosition;

in vec3 vPosition;
in vec4 aColor; // RGBA_FLOAT_COLOR
in int aMaterial; // IRIS_MATERIAL

layout (std140) uniform vertUniformBlock
{
    ivec3 uOffsetChunk;
    vec3 uOffsetSubChunk;
    ivec3 uCameraPosChunk;
    vec3 uCameraPosSubChunk;

    mat4 uProjectionMvm;
    int uSkyLight;
    int uBlockLight;

    float uNorthShading;
    float uSouthShading;
    float uEastShading;
    float uWestShading;
    float uTopShading;
    float uBottomShading;
};

uniform sampler2D uLightMap;

out vec4 fColor;

void main()
{
    vec3 aScale = vec3(1);
    
    if (aMaterial == 999)
    {
        aScale = vec3(2);
    }
    
//    vec4 aColor = texelFetch(uColorMap, ivec2(gl_InstanceID,0), 0);
//    vec3 aScale = texelFetch(uScaleMap, ivec2(gl_InstanceID,0), 0).xyz;
//    
//    float chunkX = int(texelFetch(uTranslateChunkXMap, ivec2(gl_InstanceID,0), 0).x);
//    float chunkY = int(texelFetch(uTranslateChunkYMap, ivec2(gl_InstanceID,0), 0).x);
//    float chunkZ = int(texelFetch(uTranslateChunkZMap, ivec2(gl_InstanceID,0), 0).x);
//    ivec3 aTranslateChunk = ivec3(chunkX, chunkY, chunkZ);
//    
//    vec3 aTranslateSubChunk = texelFetch(uTranslateSubChunkMap, ivec2(gl_InstanceID,0), 0).xyz;
//    int aMaterial = int(texelFetch(uMaterialMap, ivec2(gl_InstanceID,0), 0).x);
    
    // aTranslate - moves the vertex to the boxGroup's relative position
    // uOffset - moves the vertex to the boxGroup's world position
    // uCameraPos - moves the vertex into camera space
    vec3 trans = (uOffsetChunk - uCameraPosChunk) * 16.0f;
    // separate float and int values are to fix percission loss at extreme distances from the origin (IE 10,000,000+)
    // luckily large translate values minus large cameraPos generally equal values that cleanly fit in a float
    trans += (uOffsetSubChunk - uCameraPosSubChunk);
    
    // combination translation and scaling matrix
    mat4 transform = mat4(
        aScale.x, 0.0,      0.0,      0.0,
        0.0,      aScale.y, 0.0,      0.0,
        0.0,      0.0,      aScale.z, 0.0,
        trans.x,  trans.y,  trans.z,  1.0
    );
    
    gl_Position = uProjectionMvm * transform * vec4(vPosition, 1.0);

    float blockLight = (float(uBlockLight)+0.5) / 16.0;
    float skyLight = (float(uSkyLight)+0.5) / 16.0;
    vec4 lightColor = vec4(texture(uLightMap, vec2(blockLight, skyLight)).xyz, 1.0);
    
    
    fColor = lightColor * aColor;

    int vertexIndex = gl_VertexID % 24;

    // apply directional shading
    if (vertexIndex >= 0 && vertexIndex < 4) { fColor.rgb *= uNorthShading; }
    else if (vertexIndex >= 4 && vertexIndex < 8) { fColor.rgb *= uSouthShading; }
    else if (vertexIndex >= 8 && vertexIndex < 12) { fColor.rgb *= uWestShading; }
    else if (vertexIndex >= 12 && vertexIndex < 16) { fColor.rgb *= uEastShading; }
    else if (vertexIndex >= 16 && vertexIndex < 20) { fColor.rgb *= uBottomShading; }
    else if (vertexIndex >= 20 && vertexIndex < 24) { fColor.rgb *= uTopShading; }

}
