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
in vec3 vColor2;
in float vSpeed;
in float vFill;
in vec2 vTexCoord;
in vec3 vViewPos;
in vec3 vNormal;

out vec4 fragColor;

void main() {
    vec4 tex = texture(Sampler0, vTexCoord);
    if (tex.a < 0.05) discard;

    float time = GameTime * 1200.0 * vSpeed;


    float wave = 0.5 + 0.5 * sin(time * 2.0 + (vViewPos.x + vViewPos.y) * 8.0);
    vec3 col = mix(vColor1.rgb, vColor2, wave);


    float lum = dot(tex.rgb, vec3(0.299, 0.587, 0.114));
    col *= mix(0.55 + 0.75 * lum, 1.0, vFill);


    vec3 n = normalize(vNormal);
    float rim = pow(1.0 - abs(dot(n, normalize(-vViewPos))), 2.0);
    col += mix(vColor1.rgb, vColor2, 1.0 - wave) * rim * 0.7;

    fragColor = vec4(col, vColor1.a * tex.a);
}
