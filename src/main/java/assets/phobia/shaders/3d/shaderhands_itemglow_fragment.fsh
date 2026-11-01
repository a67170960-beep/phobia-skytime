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

    float time = GameTime * 1200.0 * max(vSpeed, 0.05);
    float height = 1.0 - vTexCoord.y;
    float flame = 0.55 + 0.45 * sin(time * 2.3 + vTexCoord.x * 19.0 + height * 11.0);
    float edge = pow(1.0 - abs(dot(normalize(vNormal), normalize(-vViewPos))), 2.0);
    float textureLight = dot(tex.rgb, vec3(0.299, 0.587, 0.114));

    vec3 color = mix(vColor1.rgb, vColor2, flame);
    color *= 0.65 + textureLight * 0.65;
    color += mix(vColor2, vColor1.rgb, flame) * edge * 0.8;

    float alpha = tex.a * vColor1.a * mix(0.65, 1.0, max(edge, vFill));
    fragColor = vec4(color, alpha);
}
