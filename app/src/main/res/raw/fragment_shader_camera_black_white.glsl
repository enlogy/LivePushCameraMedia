#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 ft_Position;
uniform samplerExternalOES sTexture;
void main() {
    gl_FragColor= vec4(vec3(1.0 - texture2D(sTexture, ft_Position)), 1.0);
}
