#version 330

uniform sampler2D CoreSampler;
uniform sampler2D GlowSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 core = texture(CoreSampler, texCoord);
    vec4 glow = texture(GlowSampler, texCoord);

    float coreAlpha = smoothstep(0.18, 0.72, core.a);
    // Keep the Gaussian falloff linear. Thresholding it with smoothstep
    // quantizes the halo into visible concentric layers.
    float glowAlpha = min(glow.a * 1.65, 0.62);

    vec3 coreColor = core.rgb / max(core.a, 0.001);
    vec3 glowColor = glow.rgb / max(glow.a, 0.001);

    // Proper over composition: a solid one-pixel core over a restrained halo.
    float alpha = coreAlpha + glowAlpha * (1.0 - coreAlpha);
    vec3 premultiplied = glowColor * glowAlpha * 1.22;
    premultiplied = coreColor * coreAlpha * 1.15
                  + premultiplied * (1.0 - coreAlpha);

    vec3 finalColor = premultiplied / max(alpha, 0.001);
    fragColor = vec4(finalColor, alpha);
}
