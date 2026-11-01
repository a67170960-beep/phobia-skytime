#version 410 core

in vec4 vColor;
in vec2 vUV;

out vec4 fragColor;

void main() {
    float d2 = dot(vUV, vUV);

    // One continuous bloom: no hard radius mask and no stacked visible discs.
    float hot = exp(-d2 * 54.0);
    float inner = exp(-d2 * 14.0);
    float halo = exp(-d2 * 4.6);
    float energy = (hot * 1.45 + inner * 0.58 + halo * 0.20) * vColor.a;

    if (energy < 0.0025) discard;

    vec3 color = mix(vColor.rgb, vec3(1.0), clamp(hot * 0.82, 0.0, 0.82));
    fragColor = vec4(color * energy, energy);
}
