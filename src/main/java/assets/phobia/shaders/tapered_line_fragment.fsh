#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uRect;
    float uThickness;
    float uZ;
    float uLeftTaper;
    float uRightTaper;
    float uLeftRadius;
    float uRightRadius;
    vec2 _pad;
    vec4 uColor;
};

in vec2 vUV;
in vec2 vSize;

out vec4 fragColor;

float dither(vec2 uv) {
    return (fract(sin(dot(uv.xy, vec2(12.9898, 78.233))) * 43758.5453123) - 0.5) * (1.0 / 255.0);
}

void main() {
    vec2 pixelPos = vUV * vSize;
    float x = pixelPos.x;
    float y = pixelPos.y;
    
    float halfWidth = vSize.x * 0.5;
    float halfHeight = vSize.y * 0.5;
    
    float normalizedX = x / vSize.x;
    
    float leftFactor = uLeftTaper;
    float rightFactor = uRightTaper;
    
    float taperProgress;
    float taperAmount;
    
    if (normalizedX < 0.5) {
        taperProgress = normalizedX * 2.0;
        taperAmount = mix(leftFactor, 1.0, taperProgress);
    } else {
        taperProgress = (normalizedX - 0.5) * 2.0;
        taperAmount = mix(1.0, rightFactor, taperProgress);
    }
    
    float currentHalfThickness = halfHeight * taperAmount;
    float centerY = halfHeight;
    float distFromCenter = abs(y - centerY);
    
    float alpha = 1.0;
    
    if (distFromCenter > currentHalfThickness) {
        alpha = 0.0;
    } else {
        float edgeDist = currentHalfThickness - distFromCenter;
        float edgeSoftness = fwidth(distFromCenter);
        alpha = smoothstep(0.0, edgeSoftness * 2.0, edgeDist);
    }
    
    float leftRadius = uLeftRadius;
    float rightRadius = uRightRadius;
    
    if (leftFactor > 0.01 && leftRadius > 0.0) {
        float leftDist = length(vec2(max(leftRadius - x, 0.0), distFromCenter));
        if (x < leftRadius) {
            float cornerAlpha = 1.0 - smoothstep(currentHalfThickness - fwidth(leftDist), currentHalfThickness, leftDist);
            alpha = min(alpha, cornerAlpha);
        }
    }
    
    if (rightFactor > 0.01 && rightRadius > 0.0) {
        float rightX = vSize.x - x;
        float rightDist = length(vec2(max(rightRadius - rightX, 0.0), distFromCenter));
        if (rightX < rightRadius) {
            float cornerAlpha = 1.0 - smoothstep(currentHalfThickness - fwidth(rightDist), currentHalfThickness, rightDist);
            alpha = min(alpha, cornerAlpha);
        }
    }
    
    if (alpha <= 0.0) discard;
    
    vec4 color = uColor;
    color.rgb += dither(pixelPos);
    
    fragColor = vec4(color.rgb, color.a * alpha);
}
