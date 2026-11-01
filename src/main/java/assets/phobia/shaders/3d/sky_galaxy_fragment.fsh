#version 330

layout(std140) uniform SkyData {
    mat4 InvViewProj;
    vec4 Color1;
    vec4 Color2;
    vec4 Params;
};

in vec2 vNdc;
out vec4 fragColor;

float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

float noise3(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    return mix(
        mix(mix(hash13(i), hash13(i + vec3(1, 0, 0)), u.x),
            mix(hash13(i + vec3(0, 1, 0)), hash13(i + vec3(1, 1, 0)), u.x), u.y),
        mix(mix(hash13(i + vec3(0, 0, 1)), hash13(i + vec3(1, 0, 1)), u.x),
            mix(hash13(i + vec3(0, 1, 1)), hash13(i + vec3(1, 1, 1)), u.x), u.y),
        u.z);
}

float fbm3(vec3 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise3(p);
        p = p * 2.04 + vec3(9.1, 17.3, 5.7);
        a *= 0.5;
    }
    return v;
}

float stars(vec3 d, float density, float size) {
    vec3 p = d * density;
    vec3 id = floor(p);
    float h = hash13(id);
    if (h < 0.92) return 0.0;
    vec3 off = fract(h * vec3(511.7, 269.5, 183.3)) - 0.5;
    float dist = length(fract(p) - 0.5 - off * 0.6);
    return smoothstep(size, 0.0, dist) * (0.4 + 0.6 * h);
}

void main() {
    vec4 t4 = InvViewProj * vec4(vNdc, 1.0, 1.0);
    vec3 dir = normalize(t4.xyz / t4.w);
    float time = Params.x;


    float rotA = time * 0.008;
    float c = cos(rotA);
    float s = sin(rotA);
    dir = vec3(c * dir.x + s * dir.z, dir.y, -s * dir.x + c * dir.z);

    vec3 col = vec3(0.006, 0.008, 0.018);


    float twinkle = 0.75 + 0.25 * sin(time * 2.0 + hash13(floor(dir * 150.0)) * 40.0);
    col += vec3(1.0, 0.97, 0.92) * stars(dir, 150.0, 0.13) * twinkle;
    col += vec3(0.8, 0.88, 1.0) * stars(dir + 4.2, 90.0, 0.16) * 0.7;
    col += vec3(1.0, 0.8, 0.7) * stars(dir + 8.9, 55.0, 0.20) * 0.5;


    vec3 bandNormal = normalize(vec3(0.35, 0.85, 0.25));
    float bandDist = dot(dir, bandNormal);
    float band = exp(-bandDist * bandDist * 9.0);



    float warp = noise3(dir * 7.0 + time * 0.01);
    float nebula = fbm3(dir * 4.0 + warp * 1.5);
    float dust = fbm3(dir * 9.0 + vec3(31.7));

    vec3 nebulaCol = mix(Color1.rgb, Color2.rgb, clamp(nebula * 1.6 - 0.25, 0.0, 1.0));
    col += nebulaCol * band * nebula * nebula * 2.6;


    vec3 coreDir = normalize(cross(bandNormal, vec3(1.0, 0.0, 0.3)));
    float core = pow(clamp(dot(dir, coreDir), 0.0, 1.0), 6.0);
    col += mix(vec3(1.0, 0.9, 0.75), Color2.rgb, 0.3) * core * band * 0.9;


    col -= col * band * smoothstep(0.52, 0.78, dust) * 0.55;


    col += vec3(0.95, 0.92, 0.85) * stars(dir + 13.4, 210.0, 0.15) * band * 1.2;

    col = max(col, vec3(0.0));
    fragColor = vec4(col * Params.y, 1.0);
}
