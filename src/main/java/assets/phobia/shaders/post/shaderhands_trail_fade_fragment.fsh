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

float wob(float y, float t) {
    return sin(y * 9.0 + t * 4.3) * 0.6
         + sin(y * 17.0 - t * 2.1) * 0.3
         + sin(y * 4.0 + t * 1.3) * 0.4
         + sin(y * 28.0 + t * 5.7) * 0.15;
}

vec4 blurSample(vec2 position) {
    vec2 pixel = 1.0 / max(textureSize.xy, vec2(1.0));
    vec4 color = texture(Sampler0, position) * 0.36;
    color += texture(Sampler0, position + vec2(pixel.x, 0.0)) * 0.16;
    color += texture(Sampler0, position - vec2(pixel.x, 0.0)) * 0.16;
    color += texture(Sampler0, position + vec2(0.0, pixel.y)) * 0.16;
    color += texture(Sampler0, position - vec2(0.0, pixel.y)) * 0.16;
    return color;
}

void main() {
    float time = offsetFadeTime.w;
    float dt = deltaTurbulenceFlicker.x;
    float turbulence = deltaTurbulenceFlicker.y;
    float flickerAmount = deltaTurbulenceFlicker.z;
    float turbulenceDelta = (wob(texCoord.y, time) - wob(texCoord.y, time - dt)) * turbulence;
    float yFactor = 0.7 + texCoord.y * 0.8;
    vec2 displacement = vec2(offsetFadeTime.x + turbulenceDelta, offsetFadeTime.y * yFactor);
    vec4 color = blurSample(texCoord - displacement);

    float flicker = 1.0 - flickerAmount
        + flickerAmount * (0.5 + 0.5 * sin(time * 14.0) + 0.25 * sin(time * 9.3));
    float oldAlpha = color.a;
    float newAlpha = max(0.0, oldAlpha * (0.985 * flicker) - offsetFadeTime.z);
    float scale = oldAlpha > 0.001 ? newAlpha / oldAlpha : 0.0;
    fragColor = vec4(color.rgb * scale, newAlpha);
}
