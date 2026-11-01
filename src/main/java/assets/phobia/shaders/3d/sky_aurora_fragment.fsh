#version 330

layout(std140) uniform SkyData {
    mat4 InvViewProj;
    vec4 Color1;
    vec4 Color2;
    vec4 Params;
};

in vec2 vNdc;
out vec4 fragColor;

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

float stars(vec3 d, float density, float size) {
    vec3 p = d * density;
    vec3 id = floor(p);
    float h = hash13(id);
    if (h < 0.92) return 0.0;
    vec3 off = fract(h * vec3(511.7, 269.5, 183.3)) - 0.5;
    float dist = length(fract(p) - 0.5 - off * 0.6);
    return smoothstep(size, 0.0, dist) * (0.4 + 0.6 * h);
}

mat2 mm2(float a) {
    float c = cos(a);
    float s = sin(a);
    return mat2(c, s, -s, c);
}

const mat2 M2 = mat2(0.80, 0.60, -0.60, 0.80);

float tri(float x) {
    return clamp(abs(fract(x) - 0.5), 0.01, 0.49);
}

vec2 tri2(vec2 p) {
    return vec2(tri(p.x) + tri(p.y), tri(p.y + tri(p.x)));
}


float triNoise2d(vec2 p, float spd, float time) {
    float z = 1.8;
    float z2 = 2.5;
    float rz = 0.0;
    p *= mm2(p.x * 0.06);
    vec2 bp = p;
    for (int i = 0; i < 5; i++) {
        vec2 dg = tri2(bp * 1.85) * 0.75;
        dg *= mm2(time * spd);
        p -= dg / z2;
        bp *= 1.3;
        z2 *= 0.45;
        z *= 0.42;
        p *= 1.21 + (rz - 1.0) * 0.02;
        rz += tri(p.x + tri(p.y)) * z;
        p *= -M2;
    }
    return clamp(1.0 / pow(rz * 29.0, 1.3), 0.0, 0.55);
}

void main() {
    vec4 t4 = InvViewProj * vec4(vNdc, 1.0, 1.0);
    vec3 dir = normalize(t4.xyz / t4.w);
    float time = Params.x;


    vec3 sky = mix(vec3(0.028, 0.04, 0.08), vec3(0.003, 0.006, 0.017),
                   clamp(dir.y * 1.4 + 0.25, 0.0, 1.0));


    float twinkle = 0.7 + 0.3 * sin(time * 2.5 + hash13(floor(dir * 140.0)) * 40.0);
    sky += vec3(0.9, 0.95, 1.0) * stars(dir, 140.0, 0.14) * twinkle;



    if (dir.y > -0.05) {
        vec4 aur = vec4(0.0);
        vec4 avg = vec4(0.0);
        float dither = hash12(vNdc * 971.7);

        for (int i = 0; i < 20; i++) {
            float fi = float(i);
            float of = 0.006 * dither * smoothstep(0.0, 15.0, fi);
            float pt = (0.8 + pow(fi, 1.4) * 0.002) / (dir.y * 2.0 + 0.4) - of;
            vec3 bpos = pt * dir;

            float rzt = triNoise2d(bpos.zx, 0.06, time);
            vec4 col2 = vec4(mix(Color1.rgb, Color2.rgb, fi / 20.0) * rzt, rzt);

            avg = mix(avg, col2, 0.5);
            aur += avg * exp2(-fi * 0.065 - 2.5) * smoothstep(0.0, 5.0, fi);
        }

        aur *= clamp(dir.y * 15.0 + 0.4, 0.0, 1.0);
        sky += aur.rgb * 2.4;
    }


    sky = 1.0 - exp(-sky * 1.1);

    fragColor = vec4(sky * Params.y, 1.0);
}
