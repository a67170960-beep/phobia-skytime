#version 150

in vec2 texCoord;
out vec4 fragColor;

layout(std140) uniform MenuData {
    vec4 viewportTime;
    vec4 mouseData;
};

float hash21(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash21(i), hash21(i + vec2(1.0, 0.0)), f.x),
               mix(hash21(i + vec2(0.0, 1.0)), hash21(i + 1.0), f.x), f.y);
}

float fbm(vec2 p) {
    float value = 0.0;
    float amplitude = 0.5;
    mat2 turn = mat2(0.82, -0.57, 0.57, 0.82);
    for (int i = 0; i < 4; i++) {
        value += noise(p) * amplitude;
        p = turn * p * 2.02 + 9.7;
        amplitude *= 0.5;
    }
    return value;
}

void main() {
    float time = viewportTime.z;
    float aspect = viewportTime.w;
    vec2 uv = texCoord;
    vec2 mouse = mouseData.xy - 0.5;
    vec2 p = (uv - 0.5) * vec2(aspect, 1.0);

    // Subtle parallax only: the whole scene never chases the cursor.
    vec2 parallax = mouse * vec2(0.045, 0.028);
    vec2 scene = p - parallax;

    float warpX = fbm(scene * 0.86 + vec2(time * 0.014, -time * 0.006));
    float warpY = fbm(scene * 0.91 + vec2(-time * 0.009, time * 0.011) + 5.2);
    vec2 warp = (vec2(warpX, warpY) - 0.5) * 0.72;
    float broadFog = fbm((scene + warp) * 1.34 + vec2(time * 0.008, -time * 0.005));
    float fineFog = fbm((scene - warp * 0.45) * 2.28 - vec2(time * 0.006, time * 0.008) + 8.1);
    float smoke = smoothstep(0.40, 0.77, broadFog * 0.72 + fineFog * 0.28);

    float centerBloom = exp(-dot(scene * vec2(0.62, 0.94), scene * vec2(0.62, 0.94)) * 1.16);
    float lowerBloom = exp(-dot(scene - vec2(0.0, -0.31), scene - vec2(0.0, -0.31)) * 1.72);
    float mouseBloom = exp(-dot(p - mouse * vec2(0.34, 0.24), p - mouse * vec2(0.34, 0.24)) * 7.0);

    float smokeMask = centerBloom * (0.58 + lowerBloom * 0.42);
    float softPlume = smoothstep(0.20, 0.88, smoke) * smokeMask;
    float depth = fbm(scene * 0.72 - warp * 0.35 + 2.7);

    vec3 color = vec3(0.014, 0.0144, 0.0155);
    color += vec3(0.044, 0.045, 0.050) * centerBloom;
    color += vec3(0.027, 0.028, 0.031) * lowerBloom;
    color += vec3(0.060, 0.062, 0.069) * softPlume * (0.62 + depth * 0.50);
    color += vec3(0.014, 0.015, 0.018) * mouseBloom;

    // Fine film grain keeps the dark gradient alive without stars or stripes.
    float grain = hash21(gl_FragCoord.xy + fract(time) * 71.0) - 0.5;
    color += grain * 0.0032;

    vec2 edgeUv = uv * (1.0 - uv);
    float vignette = pow(clamp(edgeUv.x * edgeUv.y * 18.0, 0.0, 1.0), 0.32);
    color *= 0.66 + vignette * 0.48;

    fragColor = vec4(color, 1.0);
}
