#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

layout(std140) uniform TrailData {
    vec4 offsetFadeTime;
    vec4 deltaTurbulenceFlicker;
    vec4 textureSize;
    vec4 trailColor1;
    vec4 trailColor2;
};

void main() {
    vec4 color = texture(Sampler0, texCoord);
    if (color.a < 0.001) {
        discard;
    }
    float gradient = clamp(texCoord.x * 0.65 + (1.0 - texCoord.y) * 0.35, 0.0, 1.0);
    vec3 tint = mix(trailColor1.rgb, trailColor2.rgb, gradient);
    float intensity = clamp(max(max(color.r, color.g), color.b) / max(color.a, 0.001), 0.45, 1.0);
    fragColor = vec4(tint * color.a * intensity, color.a);
}
