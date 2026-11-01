#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec2 Direction;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 stepUv = Direction / InSize;

    // Continuous nine-tap Gaussian (sigma 2). Integer-spaced samples avoid
    // the separated lobes/banding produced by the previous sparse kernel.
    vec4 result = texture(InSampler, texCoord) * 0.204164;
    result += texture(InSampler, texCoord + stepUv) * 0.180174;
    result += texture(InSampler, texCoord - stepUv) * 0.180174;
    result += texture(InSampler, texCoord + stepUv * 2.0) * 0.123832;
    result += texture(InSampler, texCoord - stepUv * 2.0) * 0.123832;
    result += texture(InSampler, texCoord + stepUv * 3.0) * 0.066282;
    result += texture(InSampler, texCoord - stepUv * 3.0) * 0.066282;
    result += texture(InSampler, texCoord + stepUv * 4.0) * 0.027630;
    result += texture(InSampler, texCoord - stepUv * 4.0) * 0.027630;

    fragColor = result;
}
