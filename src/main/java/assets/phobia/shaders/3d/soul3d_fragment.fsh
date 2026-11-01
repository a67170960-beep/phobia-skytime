#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uParams;
};

in vec4 vColor;
in vec2 vUV;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n) * 43758.5453123);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash(i.x + i.y * 57.0);
    float b = hash(i.x + 1.0 + i.y * 57.0);
    float c = hash(i.x + (i.y + 1.0) * 57.0);
    float d = hash(i.x + 1.0 + (i.y + 1.0) * 57.0);
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

void main() {

    float band = round(vUV.x / 4.0);
    vec2 p = vec2(vUV.x - band * 4.0, vUV.y);

    float time = uParams.x;
    float ph = hash(band + 0.731) * 6.2831853;


    float sway = noise(vec2(p.y * 2.2 - time * 2.6 + ph, band * 0.113)) - 0.5;
    p.x += sway * 0.38 * smoothstep(-0.35, 1.0, p.y);

    p.y = p.y * 0.82 - 0.06;

    float d = length(p);


    float core = exp(-d * d * 24.0);
    float inner = exp(-d * d * 5.0) * 0.50;
    float halo = exp(-d * 2.2) * 0.30;
    float edge = smoothstep(1.0, 0.5, d);
    float flicker = 0.82 + 0.18 * sin(time * 5.7 + ph * 7.3);

    float strength = (core * 1.7 + inner + halo) * edge * flicker;


    vec3 col = mix(vColor.rgb, vec3(1.0), core * 0.85);

    float alpha = strength * vColor.a;
    if (alpha < 0.004) discard;


    fragColor = vec4(col * alpha, alpha);
}
