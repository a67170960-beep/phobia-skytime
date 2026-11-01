#version 410 core

layout(std140) uniform Uniforms {
    mat4 uProjection;
    vec4 uParams;
    vec4 uCenter;
    vec4 uColor;
};

layout(location = 0) in vec3 inPosition;

out vec3 vPos;

void main() {
    vPos = inPosition;
    gl_Position = uProjection * vec4(inPosition, 1.0);
}
