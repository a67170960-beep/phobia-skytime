#version 330

layout(std140) uniform DynamicTransforms {
    mat4 ModelViewMat;
    vec4 ColorModulator;
    vec3 ModelOffset;
    mat4 TextureMat;
};

layout(std140) uniform Projection {
    mat4 ProjMat;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vColor1;
out vec3 vColor2;
out float vSpeed;
out float vFill;
out vec2 vTexCoord;
out vec3 vViewPos;
out vec3 vNormal;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    vViewPos = viewPos.xyz;
    vNormal = normalize(mat3(ModelViewMat) * Normal);
    vTexCoord = UV0;

    uint packedData = uint(UV1.x & 0xFFFF) | (uint(UV1.y & 0xFFFF) << 16u);
    vec3 primary = vec3(
        float((packedData >> 0u) & 0xFu),
        float((packedData >> 4u) & 0xFu),
        float((packedData >> 8u) & 0xFu)
    ) / 15.0;
    vec3 secondary = vec3(
        float((packedData >> 12u) & 0xFu),
        float((packedData >> 16u) & 0xFu),
        float((packedData >> 20u) & 0xFu)
    ) / 15.0;

    vSpeed = float((packedData >> 24u) & 0xFu) / 15.0 * 3.0;
    vFill = float((packedData >> 28u) & 0xFu) / 15.0;
    vColor1 = vec4(primary, vFill);
    vColor2 = secondary;
}
