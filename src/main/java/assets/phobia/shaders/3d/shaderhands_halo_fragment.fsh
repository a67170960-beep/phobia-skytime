#version 330

layout(std140) uniform Globals {
    ivec3 CameraBlockPos;
    vec3 CameraOffset;
    vec2 ScreenSize;
    float GlintAlpha;
    float GameTime;
    int MenuBlurRadius;
    int UseRgss;
};

uniform sampler2D Sampler0;

in vec4 vColor1;
in float vSpeed;
in vec2 vTexCoord;

out vec4 fragColor;

void main() {


    float a = textureLod(Sampler0, vTexCoord, 0.0).a;
    float s = smoothstep(0.35, 0.75, a);
    if (s < 0.01) discard;


    float time = GameTime * 1200.0 * vSpeed;
    float pulse = 0.86 + 0.14 * sin(time * 2.6);

    float alpha = vColor1.a * s * pulse;
    if (alpha < 0.004) discard;

    fragColor = vec4(vColor1.rgb, alpha);
}
