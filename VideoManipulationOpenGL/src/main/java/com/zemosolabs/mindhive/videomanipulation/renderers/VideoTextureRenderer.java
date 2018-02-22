package com.zemosolabs.mindhive.videomanipulation.renderers;


import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import com.zemosolabs.mindhive.videomanipulation.R;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Scanner;

public class VideoTextureRenderer extends TextureSurfaceRenderer implements SurfaceTexture.OnFrameAvailableListener
{

    private Triangle triangle;
    private static final String TAG = VideoTextureRenderer.class.getSimpleName();
    private static float yCoord = 1.0f;
    private static float xCoord = 0.5615f;
    private static float squareCoords[] = {
            -xCoord, yCoord, 0.0f,
            -xCoord, -yCoord, 0.0f,
            xCoord, -yCoord, 0.0f,
            xCoord, yCoord, 0.0f
    };

    private static short drawOrder[] = { 0, 1, 2, 0, 2, 3};

    private Context ctx;

    // Texture to be shown in background
    private FloatBuffer textureBuffer;
    private static final float textureCoords[] = new float[] { 0.0f, 1.0f, 0.0f, 1.0f,
                                      0.0f, 0.0f, 0.0f, 1.0f,
                                      1.0f, 0.0f, 0.0f, 1.0f,
                                      1.0f, 1.0f, 0.0f, 1.0f };
    private int[] textures = new int[1];

    private int vertexShaderHandle;
    private int fragmentShaderHandle;
    private int shaderProgram;
    private FloatBuffer vertexBuffer;
    private ShortBuffer drawListBuffer;

    private SurfaceTexture videoTexture;
    private float[] videoTextureTransform;
    private boolean frameAvailable = false;

    private int videoWidth;
    private int videoHeight;
    private boolean adjustViewport = false;

    private float[] mvpMatrix, projectionMatrix, viewMatrix;
    private float[] rotationMatrix, scaleMatrix, translationMatrix;
    private float[] objectMatrix;
    private int mvpMatrixHandle;
    private boolean isDefaultShader = false;

    private RendererEvents rendererEvents;

    public VideoTextureRenderer(Context context, SurfaceTexture texture, int width, int height,
                                RendererEvents rendererEvents) {
        super(texture, width, height);
        this.ctx = context;
        videoTextureTransform = new float[16];

        mvpMatrix = new float[16];
        projectionMatrix = new float[16];
        viewMatrix = new float[16];

        rotationMatrix = new float[16];
        Matrix.setIdentityM(rotationMatrix, 0);
        scaleMatrix = new float[16];
        Matrix.setIdentityM(scaleMatrix, 0);
        translationMatrix = new float[16];
        Matrix.setIdentityM(translationMatrix, 0);

        objectMatrix = new float[16];
        Matrix.setIdentityM(objectMatrix, 0);
        buildObjectMatrix();

        this.rendererEvents = rendererEvents;
    }

    private void loadShaders(Context context)
    {
        InputStream vertexShaderStream = context.getResources().openRawResource(R.raw.vertex_shader);
        InputStream fragmentShaderStream = context.getResources().openRawResource(R.raw.fragment_shader);

        String vertexShaderCode = getStringFromStream(vertexShaderStream);
        String fragmentShaderCode = getStringFromStream(fragmentShaderStream);

        vertexShaderHandle = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER);
        GLES20.glShaderSource(vertexShaderHandle, vertexShaderCode);
        GLES20.glCompileShader(vertexShaderHandle);
        checkGlError("Vertex shader compile");

        fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
        GLES20.glShaderSource(fragmentShaderHandle, fragmentShaderCode);
        GLES20.glCompileShader(fragmentShaderHandle);
        checkGlError("Pixel shader compile");

        IntBuffer intBuffer = ByteBuffer.allocateDirect(4)
                .order(ByteOrder.nativeOrder()).asIntBuffer();
        GLES20.glGetShaderiv(fragmentShaderHandle, GLES20.GL_COMPILE_STATUS, intBuffer);
        int compilationStatus = intBuffer.get(0);

        if(compilationStatus != 1) {
            Log.d(TAG, "Compiling default Shader");
            isDefaultShader = true;
            InputStream defaultFragmentShader = context.getResources().openRawResource(R.raw.fragment_shader_default);
            String defaultFragmentShaderCode = getStringFromStream(defaultFragmentShader);
            fragmentShaderHandle = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER);
            GLES20.glShaderSource(fragmentShaderHandle, defaultFragmentShaderCode);
            GLES20.glCompileShader(fragmentShaderHandle);
            checkGlError("Pixel shader compile");
        }


        shaderProgram = GLES20.glCreateProgram();
        GLES20.glAttachShader(shaderProgram, vertexShaderHandle);
        GLES20.glAttachShader(shaderProgram, fragmentShaderHandle);
        GLES20.glLinkProgram(shaderProgram);
        checkGlError("Shader program compile");

        int[] status = new int[1];
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, status, 0);
        if (status[0] != GLES20.GL_TRUE) {
            String error = GLES20.glGetProgramInfoLog(shaderProgram);
            Log.e("SurfaceTest", "Error while linking program:\n" + error);
        }

    }


    private void setupVertexBuffer()
    {
        // Draw list buffer
        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder. length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        // Initialize the texture holder
        ByteBuffer bb = ByteBuffer.allocateDirect(squareCoords.length * 4);
        bb.order(ByteOrder.nativeOrder());

        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(squareCoords);
        vertexBuffer.position(0);
    }


    private void setupTexture(Context context)
    {
        ByteBuffer texturebb = ByteBuffer.allocateDirect(textureCoords.length * 4);
        texturebb.order(ByteOrder.nativeOrder());

        textureBuffer = texturebb.asFloatBuffer();
        textureBuffer.put(textureCoords);
        textureBuffer.position(0);

        // Generate the actual texture
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glGenTextures(1, textures, 0);
        checkGlError("Texture generate");

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        checkGlError("Texture bind");

        videoTexture = new SurfaceTexture(textures[0]);
        videoTexture.setOnFrameAvailableListener(this);
    }

    @Override
    protected boolean draw()
    {
        synchronized (this)
        {
            if (frameAvailable)
            {
                videoTexture.updateTexImage();
                videoTexture.getTransformMatrix(videoTextureTransform);
                frameAvailable = false;
            }
            else
            {
                return false;
            }

        }

//        if (adjustViewport) {
//            adjustViewport();
//        }

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Draw texture
        GLES20.glUseProgram(shaderProgram);
        int textureParamHandle = GLES20.glGetUniformLocation(shaderProgram, "texture");
        int textureCoordinateHandle = GLES20.glGetAttribLocation(shaderProgram, "vTexCoordinate");
        int positionHandle = GLES20.glGetAttribLocation(shaderProgram, "vPosition");
        int textureTranformHandle = GLES20.glGetUniformLocation(shaderProgram, "textureTransform");
        int brightnessHandle = GLES20.glGetUniformLocation(shaderProgram, "brightness");
        int saturationHandle = GLES20.glGetUniformLocation(shaderProgram, "saturation");
        int isBlurHandle = GLES20.glGetUniformLocation(shaderProgram, "isBlur");

        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 4 * 3, vertexBuffer);

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glUniform1i(textureParamHandle, 0);

        GLES20.glEnableVertexAttribArray(textureCoordinateHandle);
        GLES20.glVertexAttribPointer(textureCoordinateHandle, 4, GLES20.GL_FLOAT, false, 0, textureBuffer);


        float ratio = (float) width / height;
        GLES20.glViewport(-1, -1, width, height);

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, 3, 7);

        Matrix.setLookAtM(viewMatrix, 0, 0, 0, 3, 0, 0, 0, 0, 1, 0);

        GLES20.glUniform1f(brightnessHandle, brightness);
        GLES20.glUniform1f(saturationHandle, saturation);
        GLES20.glUniform1i(isBlurHandle, 0);

        rendererEvents.onDrawFrame();

        buildObjectMatrix();

        float[] tempM = new float[16];
        Matrix.multiplyMM(tempM, 0, projectionMatrix, 0, viewMatrix, 0);
        Matrix.multiplyMM(mvpMatrix, 0, tempM, 0, objectMatrix, 0);

        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram, "uMVPMatrix");

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        GLES20.glUniformMatrix4fv(textureTranformHandle, 1, false, videoTextureTransform, 0);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length, GLES20.GL_UNSIGNED_SHORT, drawListBuffer);
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(textureCoordinateHandle);
        triangle.draw();
        return true;
    }

    private void adjustViewport()
    {
        float surfaceAspect = height / (float)width;
        float videoAspect = videoHeight / (float)videoWidth;

        if (surfaceAspect > videoAspect)
        {
            float heightRatio = height / (float) videoHeight;
            int newWidth = (int)(width * heightRatio);
            int xOffset = (newWidth - width) / 2;
            GLES20.glViewport(-xOffset, 0, newWidth, height);
            Log.v("Dimensions", "viewport: " + newWidth + " " + height);
        }
        else
        {
            float widthRatio = width / (float)videoWidth;
            int newHeight = (int)(height * widthRatio);
            int yOffset = (newHeight - height) / 2;
            GLES20.glViewport(0, -yOffset, width, newHeight);
            Log.v("Dimensions", "viewport: " + width + " " + newHeight);
        }

        adjustViewport = false;
    }

    @Override
    protected void initGLComponents()
    {
        setupVertexBuffer();
        setupTexture(ctx);
        loadShaders(ctx);
        triangle = new Triangle();
        rendererEvents.onInitialized();
    }

    @Override
    protected void deinitGLComponents()
    {
        GLES20.glDeleteTextures(1, textures, 0);
        GLES20.glDeleteProgram(shaderProgram);
        videoTexture.setOnFrameAvailableListener(null);
        videoTexture.release();
        videoTexture = null;
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public void setVideoSize(int width, int height)
    {
        this.videoWidth = width;
        this.videoHeight = height;
        adjustViewport = true;
    }

    public void checkGlError(String op)
    {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e("SurfaceTest", op + ": glError " + GLUtils.getEGLErrorString(error));
        }
    }

    public SurfaceTexture getVideoTexture()
    {
        return videoTexture;
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture)
    {
        synchronized (this)
        {
            frameAvailable = true;
        }
    }

    private void buildObjectMatrix() {
        float[] tempM = new float[16];
        Matrix.multiplyMM(tempM, 0, translationMatrix, 0, rotationMatrix, 0);
        Matrix.multiplyMM(objectMatrix, 0, tempM, 0, scaleMatrix, 0);
    }

    public void setPanCoords(float x, float y) {
        float[] tempPanM = new float[16];
        Matrix.setIdentityM(tempPanM, 0);
        Matrix.translateM(tempPanM, 0, x, y, 0);
        translationMatrix = Arrays.copyOf(tempPanM, 16);
    }

    public void setScaleFactor(float scaleFactor) {
        float[] tempScaleM = new float[16];
        Matrix.setIdentityM(tempScaleM, 0);
        Matrix.scaleM(tempScaleM, 0, scaleFactor, scaleFactor, 0);
        scaleMatrix = Arrays.copyOf(tempScaleM, 16);
    }

    public void setAngle(float angle) {
        float[] tempRotationM = new float[16];
        Matrix.setIdentityM(tempRotationM, 0);
        Matrix.setRotateM(tempRotationM, 0, angle, 0, 0, 1);
        rotationMatrix = Arrays.copyOf(tempRotationM, 16);
    }

    private float brightness = 0.5f, saturation = -0.5f;

    public void setBrightness(float brightness) {
        this.brightness = brightness;
    }

    public void setSaturation(float saturation) {
        // subtracting 1 from saturation because G's color algorithm works on [-1, 1]
        if(isDefaultShader) {
            this.saturation = saturation;
        } else {
            this.saturation = (saturation - 1);
        }
    }

    public void drawFrame() {
        frameAvailable = true;
    }

    private String getStringFromStream(InputStream inputStream) {
        Scanner s = new Scanner(inputStream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";

        return result;
    }

    public interface RendererEvents {
        void onInitialized();
        void onDrawFrame();
    }
}
