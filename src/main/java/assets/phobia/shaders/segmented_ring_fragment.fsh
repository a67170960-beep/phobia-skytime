#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRect;
    vec4 uParams;
    vec4 uColors[3];
};

in vec2 vUV;
in vec2 vSize;
out vec4 fragColor;

float angleDistance(float a, float b) {
    return abs(mod(a - b + 180.0, 360.0) - 180.0);
}

void main() {
    vec2 point = (vUV - 0.5) * vSize;
    float radius = length(point);
    float outerRadius = uRect.z * 0.5;
    float innerRadius = outerRadius - uParams.x;
    float radialEdge = max(fwidth(radius), 0.65);

    float radialAlpha = smoothstep(innerRadius - radialEdge, innerRadius + radialEdge, radius)
                      * (1.0 - smoothstep(outerRadius - radialEdge, outerRadius + radialEdge, radius));

    float angle = degrees(atan(point.y, point.x));
    float distances[3] = float[](
        angleDistance(angle, 90.0),
        angleDistance(angle, -30.0),
        angleDistance(angle, -150.0)
    );

    int sector = 0;
    float distanceToCenter = distances[0];
    if (distances[1] < distanceToCenter) {
        sector = 1;
        distanceToCenter = distances[1];
    }
    if (distances[2] < distanceToCenter) {
        sector = 2;
        distanceToCenter = distances[2];
    }

    float halfSector = uParams.y * 0.5;
    float angularEdge = max(fwidth(angle), 0.65);
    float angularAlpha = 1.0 - smoothstep(
        halfSector - angularEdge,
        halfSector + angularEdge,
        distanceToCenter
    );
    float alpha = radialAlpha * angularAlpha;
    if (alpha <= 0.0) discard;

    vec4 color = uColors[sector];
    fragColor = vec4(color.rgb, color.a * alpha);
}
