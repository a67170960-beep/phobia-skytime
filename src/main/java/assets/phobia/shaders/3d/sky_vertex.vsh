#version 330

in vec3 inPosition;

out vec2 vNdc;

void main() {
    vNdc = inPosition.xy;
    gl_Position = vec4(inPosition.xy, 0.0, 1.0);
}
