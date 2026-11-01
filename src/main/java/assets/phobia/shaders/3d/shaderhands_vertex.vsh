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



    int lu = UV2.x & 0xFFFF;
    int lv = UV2.y & 0xFFFF;
    vColor1 = vec4(float((lu >> 8) & 0xFF), float(lu & 0xFF), float((lv >> 8) & 0xFF), float(lv & 0xFF)) / 255.0;



    int u = UV1.x & 0xFFFF;
    int v = UV1.y & 0xFFFF;
    vColor2 = vec3(float((u >> 8) & 0xFF), float(u & 0xFF), float((v >> 8) & 0xFF)) / 255.0;
    vSpeed = float((v >> 4) & 0xF) / 15.0 * 3.0;
    vFill = float(v & 0xF) / 15.0;
}
