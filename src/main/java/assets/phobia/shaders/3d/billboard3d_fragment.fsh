#version 410 core

uniform sampler2D Sampler0;

in vec4 vColor;
in vec2 vUV;
out vec4 fragColor;

void main() {
    vec4 sampled = texture(Sampler0, vUV) * vColor;
    if (sampled.a < 0.005) discard;
    fragColor = sampled;
}
