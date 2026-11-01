#version 330

uniform sampler2D InSampler;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / InSize;
    vec4 center = texture(InSampler, texCoord);

    vec4 n0 = texture(InSampler, texCoord + vec2(-px.x,  0.0));
    vec4 n1 = texture(InSampler, texCoord + vec2( px.x,  0.0));
    vec4 n2 = texture(InSampler, texCoord + vec2( 0.0, -px.y));
    vec4 n3 = texture(InSampler, texCoord + vec2( 0.0,  px.y));
    vec4 n4 = texture(InSampler, texCoord + vec2(-px.x, -px.y));
    vec4 n5 = texture(InSampler, texCoord + vec2( px.x, -px.y));
    vec4 n6 = texture(InSampler, texCoord + vec2(-px.x,  px.y));
    vec4 n7 = texture(InSampler, texCoord + vec2( px.x,  px.y));

    // Morphological outer edge: only pixels outside the model are emitted.
    // This avoids the doubled inner/outer Sobel line and keeps the core crisp.
    float neighborAlpha = max(max(max(n0.a, n1.a), max(n2.a, n3.a)),
                              max(max(n4.a, n5.a), max(n6.a, n7.a)));
    float edge = smoothstep(0.02, 0.72, neighborAlpha) * (1.0 - step(0.02, center.a));

    vec3 colorSum = n0.rgb * n0.a + n1.rgb * n1.a + n2.rgb * n2.a + n3.rgb * n3.a
                  + n4.rgb * n4.a + n5.rgb * n5.a + n6.rgb * n6.a + n7.rgb * n7.a;
    float colorWeight = n0.a + n1.a + n2.a + n3.a + n4.a + n5.a + n6.a + n7.a;
    vec3 edgeColor = colorSum / max(colorWeight, 0.001);

    // Premultiplied output prevents dark fringes during the blur passes.
    fragColor = vec4(edgeColor * edge, edge);
}
