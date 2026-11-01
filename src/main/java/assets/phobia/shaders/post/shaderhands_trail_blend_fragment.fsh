#version 150

in vec2 texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;

void main() {
    vec4 color = texture(Sampler0, texCoord);
    if (color.a < 0.001) {
        discard;
    }
    fragColor = vec4(color.rgb * color.a, color.a);
}
