#version 330

layout(std140) uniform SkyData {
    mat4 InvViewProj;
    vec4 Color1;
    vec4 Color2;
    vec4 Params;
};

in vec2 vNdc;
out vec4 fragColor;


const vec3 BH_DIR = normalize(vec3(0.2, 0.45, -1.0));

float hash12(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float hash13(vec3 p3) {
    p3 = fract(p3 * 0.1031);
    p3 += dot(p3, p3.zyx + 31.32);
    return fract((p3.x + p3.y) * p3.z);
}

float noise2(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash12(i), hash12(i + vec2(1.0, 0.0)), u.x),
               mix(hash12(i + vec2(0.0, 1.0)), hash12(i + vec2(1.0, 1.0)), u.x), u.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise2(p);
        p = p * 2.03 + vec2(11.3, 7.9);
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

vec2 rot(vec2 p, float a) {
    float c = cos(a);
    float s = sin(a);
    return vec2(c * p.x - s * p.y, s * p.x + c * p.y);
}

void main() {
    vec4 t4 = InvViewProj * vec4(vNdc, 1.0, 1.0);
    vec3 dir = normalize(t4.xyz / t4.w);
    float time = Params.x;


    vec3 ux = normalize(cross(BH_DIR, vec3(0.0, 1.0, 0.0)));
    vec3 uy = normalize(cross(ux, BH_DIR));

    float cosA = clamp(dot(dir, BH_DIR), -1.0, 1.0);
    float a = acos(cosA);
    vec3 perp = dir - BH_DIR * cosA;
    float perpLen = length(perp);
    vec3 pn = perpLen > 1e-5 ? perp / perpLen : ux;

    float rs = 0.12;


    float bend = 0.030 / max(a, 0.015);
    float aLensed = a - bend;
    vec3 lensedDir = BH_DIR * cos(aLensed) + pn * sin(aLensed);


    vec3 col = vec3(0.006, 0.008, 0.016);
    col += vec3(0.9, 0.95, 1.0) * stars(lensedDir, 130.0, 0.14);
    col += vec3(0.7, 0.8, 1.0) * stars(lensedDir + 3.7, 70.0, 0.18) * 0.6;
    float neb = fbm(lensedDir.xy * 3.0 + lensedDir.z * 2.0);
    col += Color2.rgb * neb * neb * 0.10;



    vec2 q = vec2(dot(dir, ux), dot(dir, uy));
    vec2 dq = vec2(q.x, q.y * 3.4);
    float dr = length(dq);

    float diskMask = smoothstep(rs * 1.10, rs * 1.55, dr) * smoothstep(rs * 4.2, rs * 1.85, dr);

    vec2 swirlUv = rot(dq / rs, -time * 0.6 + dr * 9.0);
    float swirl = fbm(swirlUv * 3.5 - vec2(time * 0.4, 0.0));

    float doppler = 1.0 + 0.55 * (dq.x / max(dr, 1e-4));

    float heat = smoothstep(rs * 4.2, rs * 1.6, dr);

    vec3 diskCol = mix(Color1.rgb, Color2.rgb, clamp(swirl * 1.5 - 0.1, 0.0, 1.0));
    float disk = diskMask * (0.30 + 1.0 * swirl) * doppler * (0.5 + 0.7 * heat);
    col += diskCol * disk * 1.6;


    float ring = exp(-pow((a - rs) * 150.0, 2.0));
    ring += 0.30 * exp(-pow((a - rs) * 45.0, 2.0));
    col += mix(vec3(1.0, 0.9, 0.7), Color1.rgb, 0.35) * ring * 1.7;


    float hole = smoothstep(rs * 0.99, rs * 0.90, a);
    col *= 1.0 - hole;


    col = 1.0 - exp(-col * 1.25);

    fragColor = vec4(col * Params.y, 1.0);
}
