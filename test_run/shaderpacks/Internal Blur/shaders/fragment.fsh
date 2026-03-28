#version 120
uniform sampler2D texture;
uniform vec2 texSize;
varying vec2 v_texCoord;
void main() {
    float blurSize = 1.0 / texSize.x * 2.0;
    vec4 sum = vec4(0.0);
    sum += texture2D(texture, vec2(v_texCoord.x - 4.0*blurSize, v_texCoord.y)) * 0.05;
    sum += texture2D(texture, vec2(v_texCoord.x - 3.0*blurSize, v_texCoord.y)) * 0.09;
    sum += texture2D(texture, vec2(v_texCoord.x - 2.0*blurSize, v_texCoord.y)) * 0.12;
    sum += texture2D(texture, vec2(v_texCoord.x - blurSize, v_texCoord.y)) * 0.15;
    sum += texture2D(texture, v_texCoord) * 0.16;
    sum += texture2D(texture, vec2(v_texCoord.x + blurSize, v_texCoord.y)) * 0.15;
    sum += texture2D(texture, vec2(v_texCoord.x + 2.0*blurSize, v_texCoord.y)) * 0.12;
    sum += texture2D(texture, vec2(v_texCoord.x + 3.0*blurSize, v_texCoord.y)) * 0.09;
    sum += texture2D(texture, vec2(v_texCoord.x + 4.0*blurSize, v_texCoord.y)) * 0.05;
    gl_FragColor = sum;
}