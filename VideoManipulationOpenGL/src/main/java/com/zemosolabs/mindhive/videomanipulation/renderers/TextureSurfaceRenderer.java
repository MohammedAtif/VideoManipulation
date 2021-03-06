package com.zemosolabs.mindhive.videomanipulation.renderers;

import android.graphics.SurfaceTexture;
import android.opengl.GLUtils;
import android.util.Log;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

/**
 * Renderer which initializes OpenGL 2.0 context on a passed surface and starts a rendering thread
 * This class has to be subclassed to be used properly
 */
public abstract class TextureSurfaceRenderer implements Runnable {
    private final static String TAG = TextureSurfaceRenderer.class.getSimpleName();
    private static final int EGL_OPENGL_ES2_BIT = 4;
    private static final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;
    private static final String LOG_TAG = "SurfaceTest.GL";
    protected final SurfaceTexture texture;
    private EGL10 egl;
    private EGLDisplay eglDisplay;
    private EGLContext eglContext;
    private EGLSurface eglSurface;

    protected int width;
    protected int height;
    private volatile boolean running;

    private Thread textureSurfaceThread;

    /**
     * @param texture Surface texture on which to render. This has to be called AFTER the texture became available
     * @param width Width of the passed surface
     * @param height Height of the passed surface
     */
    TextureSurfaceRenderer(SurfaceTexture texture, int width, int height) {
        this.texture = texture;
        this.width = width;
        this.height = height;
        this.running = true;
        textureSurfaceThread = new Thread(this);
        textureSurfaceThread.start();
    }

    @Override
    public void run() {
        initGL();
        initGLComponents();
        Log.d(LOG_TAG, "OpenGL init OK.");

        while (running) {
            Thread currentThread = Thread.currentThread();
            if(currentThread != textureSurfaceThread){
                break;
            }
            long loopStart = System.currentTimeMillis();

            if (draw()) {
                egl.eglSwapBuffers(eglDisplay, eglSurface);
            }

            long waitDelta = 33 - (System.currentTimeMillis() - loopStart) % 33;    // Targeting 30 fps, no need for faster
            if (waitDelta > 0) {
                try {
                    Thread.sleep(waitDelta);
                }
                catch (InterruptedException e) {
                    Log.e(TAG, "Couldn't sleep thread", e);
                }
            }
        }
        deinitGLComponents();
        deinitGL();
    }

    /**
     * Main draw function, subclass this and add custom drawing code here. The rendering thread will attempt to limit
     * FPS to 60 to keep CPU usage low.
     */
    protected abstract boolean draw();

    /**
     * OpenGL component initialization function. This is called after OpenGL context has been initialized on the rendering thread.
     * Subclass this and initialize shaders / textures / other GL related components here.
     */
    protected abstract void initGLComponents();
    protected abstract void deinitGLComponents();

    /**
     * Call when activity resumes, This restarts the thread.
     */
    public void onResume() {
        Log.d(TAG, "Surface renderer resumed, already Running ? "+running);
        if(!running) {
            this.running = true;
            textureSurfaceThread = new Thread(this);
            textureSurfaceThread.start();
        }
    }

    /**
     * Call when activity pauses. This stops the rendering thread.
     */
    public void onPause() {
        Log.d(TAG, "Surface renderer paused");
        this.running = false;
        this.textureSurfaceThread = null;
    }


    private void initGL() {
        egl = (EGL10) EGLContext.getEGL();
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY);

        int[] version = new int[2];
        egl.eglInitialize(eglDisplay, version);

        EGLConfig eglConfig = chooseEglConfig();
        eglContext = createContext(egl, eglDisplay, eglConfig);

        eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, texture, null);

        if ((eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) && running) {
            throw new RuntimeException("Texture is : "+texture+" GL Error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }else if(!running){
            Log.e(TAG, "renderer paused, couldn't generate the surface");
            return;
        }

        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext) && running) {
            throw new RuntimeException("GL Make current error: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }else if(!running){
            Log.e(TAG, "renderer paused, couldn't make current");
        }
    }

    private void deinitGL() {
        egl.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT);
        egl.eglDestroySurface(eglDisplay, eglSurface);
        egl.eglDestroyContext(eglDisplay, eglContext);
        egl.eglTerminate(eglDisplay);
        Log.d(LOG_TAG, "OpenGL deinit OK.");
    }

    private EGLContext createContext(EGL10 egl, EGLDisplay eglDisplay, EGLConfig eglConfig) {
        int[] attribList = { EGL_CONTEXT_CLIENT_VERSION, 2, EGL10.EGL_NONE };
        return egl.eglCreateContext(eglDisplay, eglConfig, EGL10.EGL_NO_CONTEXT, attribList);
    }

    private EGLConfig chooseEglConfig() {
        int[] configsCount = new int[1];
        EGLConfig[] configs = new EGLConfig[1];
        int[] configSpec = getConfig();

        if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, configsCount)) {
            throw new IllegalArgumentException("Failed to choose config: " + GLUtils.getEGLErrorString(egl.eglGetError()));
        }
        else if (configsCount[0] > 0) {
            return configs[0];
        }

        return null;
    }

    private int[] getConfig() {
        return new int[] {
                EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                EGL10.EGL_RED_SIZE, 8,
                EGL10.EGL_GREEN_SIZE, 8,
                EGL10.EGL_BLUE_SIZE, 8,
                EGL10.EGL_ALPHA_SIZE, 8,
                EGL10.EGL_DEPTH_SIZE, 0,
                EGL10.EGL_STENCIL_SIZE, 0,
                EGL10.EGL_NONE
        };
    }

    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        running = false;
    }
}