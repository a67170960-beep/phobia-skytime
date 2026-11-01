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

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

out vec4 vColor1;
out float vSpeed;
out vec2 vTexCoord;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    vec4 clip = ProjMat * viewPos;


    int lu = UV2.x & 0xFFFF;
    int lv = UV2.y & 0xFFFF;
    vColor1 = vec4(float((lu >> 8) & 0xFF), float(lu & 0xFF), float((lv >> 8) & 0xFF), float(lv & 0xFF)) / 255.0;



    int u = UV1.x & 0xFFFF;
    int v = UV1.y & 0xFFFF;
    float angle = float((u >> 8) & 0xFF) / 255.0 * 6.28318530718;
    float radius = float(u & 0xFF) / 4.0;
    vSpeed = float((v >> 4) & 0xF) / 15.0 * 3.0;

    if (radius > 0.01) {

        vec2 dir = vec2(cos(angle), sin(angle));
        clip.xy += dir * radius * (2.0 / ScreenSize) * clip.w;


        clip.z += 0.002 * clip.w;
    }

    gl_Position = clip;
    vTexCoord = UV0;
}
