precision mediump float;
varying vec2 vTextureCoord;

uniform sampler2D sTexture; // normal texture
uniform sampler2D bTexture; // blur texture
uniform vec2 center; // width, height / 2.0

void main() {
   gl_FragColor = vec4(texture2D(sTexture, vTextureCoord).rgb, 1.0);
}
