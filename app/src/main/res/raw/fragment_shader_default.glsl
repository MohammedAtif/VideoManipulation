#extension GL_OES_EGL_image_external : require

precision mediump float;
uniform samplerExternalOES texture;
uniform float brightness;
uniform float saturation;
uniform bool isBlur;
varying vec2 v_TexCoordinate;

vec4 horizontalBlur(vec4 color) {
    return vec4(0, 0, 0, 0);
}

vec4 verticalBlur(vec4 color) {
    return vec4(0, 0, 0, 0);
}

vec4 blur(vec4 color) {
    vec4 horizontalBlurColor = horizontalBlur(color);
    return verticalBlur(horizontalBlurColor);
}

void main () {
	const vec3 W = vec3(0.2125, 0.7154, 0.0721);
	vec4 color = texture2D(texture, v_TexCoordinate);

    lowp float luminance = dot(color.xyz, W);
    lowp vec3 grayScaleColor = vec3(luminance);

	vec4 saturatedColor = vec4(mix(grayScaleColor, color.xyz, saturation), color.w);
	vec4 brightenedColor = saturatedColor + vec4(brightness, brightness, brightness, 0);

	color = brightenedColor;

	if(isBlur) {
	    color = blur(brightenedColor);
	}

    //gl_FragColor is internal variable used by GLES to update the shader color
	gl_FragColor = color;
//	gl_FragColor = saturatedColor + vec4(brightness, brightness, brightness, 0.0);
//  vec4 color = texture2D(texture, v_TexCoordinate);
//  gl_FragColor = color;
}

