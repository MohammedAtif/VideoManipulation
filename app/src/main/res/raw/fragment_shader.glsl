#extension GL_OES_EGL_image_external : require

precision mediump float;
uniform samplerExternalOES texture;
uniform float brightness;
uniform float saturation;
uniform bool isBlur;
varying vec2 v_TexCoordinate;

//enum { ry, yg, gc, cb, bm, mr, wk };

int ry = 1000;
int yg = 1001;
int gc = 1002;
int cb = 1003;
int bm = 1004;
int mr = 1005;
int wk = 1006;

vec4 applyColorTransform(vec4 rgb, float br, float sa) {
    float r = rgb.x;
    float g = rgb.y;
    float b = rgb.z;

    float x, y, z;
    int p;


    // permute rgb to xyzp
    if( r==g && r==b ) {
        p = wk; x = r; z = b; y = g;
    } else if( r >= g ) {
        if( g>=b ) {
            p = ry; x = r; z = g; y = b;
        } else {
            if( b>=r ) {
                p = bm; x = b; z = r; y = g;
            } else {
                p = mr; x = r; z = b; y = g;
            }
        }
    } else {
        if( r>=b ) {
            p = yg; x = g; z = r; y = b;
        } else {
            if( g>=b ) {
                p = gc; x = g; z = b; y = r;
            } else {
                p = cb; x = b; z = g; y = r;
            }
        }
    }

    // compute relative z
    z = x > y ? (z-y)/(x-y) : 1.0f;
    sa *= 0.95;
    br *= 0.95;

    // transform x,y to c,n
    float c,n;
    n = 0.5*(x+y);
    float d = x+y-2.0f*x*y;
//    c = d>0.0f ? (x-y)/d : 0.0f;
    c = d > 0.0f && d < 1.0 ? (x-y) / d : 0.0f;

    // adapt the boundary conditions
//    const float m = 0.05;
//    c = sa > 0.0f ? (c+sa*m)/(1.0f+sa*m) : c / (1.0f-sa*m);
//    n = br > 0.0f ? (n+br*m)/(1.0f+br*m) : n / (1.0f-br*m);

    //  apply the c transform
    float q = 1.0f - sa * (1.0f - 2.0f * c);
    c = q > 0.0f ? c * (1.0f + sa)/q : 0.0f;

    //  apply the n transform
    q = 1.0f - br * (1.0f - 2.0f * n);
    n = q > 0.0f ? n * (1.0f + br) / q : 0.0f;

    // transform c,n to x,y
    if( c < 0.1f ) {
        x = y = n;
    }
    else {
        float u = 1.0f / c;
        float b = 2.0f * n + u;
        float s = sqrt(b * b - 4.0f * n * (1.0f + u));
        x = 0.5f * (b - s);
        y = 2.0 * n - x;
    }

    // compute final z
    z = y + (x-y)*z;

    // unpermute xyzp to rgb
    if(p == ry) {
        r = x; g = z; b = y;
    } else if (p == bm) {
        r = z; g = y; b = x;
    } else if (p == mr) {
        r = x; g = y; b = z;
    } else if (p == yg) {
        r = z; g = x; b = y;
    } else if (p == gc) {
        r = y; g = x; b = z;
    } else if (p == cb) {
        r = y; g = z; b = x;
    } else {
        r = x; g = z; b = y;
    }
//        switch(p) {
//            case ry:    r = x; g = z; b = y;	break;
//            case bm:    r = z; g = y; b = x;	break;
//            case mr: 	r = x; g = y; b = z;	break;
//            case yg: 	r = z; g = x; b = y;	break;
//            case gc: 	r = y; g = x; b = z;	break;
//            case cb:    r = y; g = z; b = x;	break;
//            case wk:    r = x; g = z; b = y;	break;
//        }

//        rgb.r = r;
//        rgb.g = g;
//        rgb.b = b;
    return vec4(r, g, b, rgb.w);
}

void main () {
	const vec4 W = vec4(0.2125, 0.7154, 0.0721, 0.0);
	vec4 color = texture2D(texture, v_TexCoordinate);

//	vec4 intensity = vec4(dot(color, W));
//	vec4 saturatedColor = mix(intensity, color, saturation);
//	vec4 brightenedColor = saturatedColor + vec4(brightness, brightness, brightness, 0.0);

//  gl_FragColor is internal variable used by GLES to update the shader color
//	gl_FragColor = brightenedColor;

	gl_FragColor = applyColorTransform(color, brightness, saturation); // Not calling george's color transform code.


//	gl_FragColor = saturatedColor + vec4(brightness, brightness, brightness, 0.0);
//  vec4 color = texture2D(texture, v_TexCoordinate);
//  gl_FragColor = color;
}

