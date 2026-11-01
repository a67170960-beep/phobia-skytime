#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uParams;
    vec4 uCenter;
    vec4 uColor;
};

in vec3 vPos;

out vec4 fragColor;

void main() {
    vec3 rd = normalize(vPos);
    vec3 c = uCenter.xyz;
    float R = uParams.y;
    float r = uParams.z;
    float time = uParams.x;


    float tc = max(dot(c, rd), 0.0);
    float span = R + r * 8.0;
    float t0 = max(0.05, tc - span);
    float t1 = tc + span;

    const int STEPS = 30;
    float dt = (t1 - t0) / float(STEPS);
    float glow = 0.0;
    float hot = 0.0;
    float t = t0 + dt * 0.5;

    for (int i = 0; i < STEPS; i++) {
        vec3 p = rd * t - c;

        vec2 q = vec2(length(p.xz) - R, p.y);
        float d2 = dot(q, q);


        float ang = atan(p.z, p.x);
        float sweep = 0.55 + 0.45 * sin(ang * 2.0 + time * 2.8);

        glow += exp(-d2 / (r * r)) * sweep;
        hot  += exp(-d2 / (r * r * 0.10)) * sweep;
        t += dt;
    }

    float norm = dt / r;
    glow *= norm;
    hot *= norm;

    float alpha = clamp(glow * 0.50 + hot * 0.50, 0.0, 1.25) * uParams.w;
    if (alpha < 0.004) discard;


    vec3 col = mix(uColor.rgb, vec3(1.0), clamp(hot * 0.45, 0.0, 0.8));


    fragColor = vec4(col * alpha, alpha);
}
