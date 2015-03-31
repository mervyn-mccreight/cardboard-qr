package de.fhwedel.google.cardboardprojekt;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES20;
import android.os.Bundle;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import de.fhwedel.google.cardboardprojekt.gl.ShaderCodeLoader;


public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer, SurfaceTexture.OnFrameAvailableListener {

    // number of coordinates per vertex in this array
    private static final int COORDS_PER_VERTEX = 2;
    private static final float SQUARE_VERTICES[] = { // in counterclockwise order:
            -1.0f, -1.0f,
            1.0f, -1.0f,
            -1.0f, 1.0f,
            1.0f, 1.0f,
    };

    private static final float TEXTURE_VERTICES[] = {
            0.0f, 1.0f,
            1.0f, 1.0f,
            0.0f, 0.0f,
            1.0f, 0.0f
    };

    private static final short[] drawOrder = {0, 2, 1, 1, 2, 3};

    private static final int GL_TEXTURE_EXTERNAL_OES = 0x8D65;

    private CardboardView cardboardView;
    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVerticesBuffer;
    private SurfaceTexture surface;
    private int program;
    private int texture;
    private ShortBuffer drawListBuffer;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Associate a CardboardView.StereoRenderer with cardboardView.
        cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        // Associate the cardboardView with this activity.
        setCardboardView(cardboardView);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        //todo: why?
        surface.updateTexImage();
    }

    @Override
    public void onDrawEye(Eye eye) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        GLES20.glUseProgram(program);

        GLES20.glActiveTexture(GL_TEXTURE_EXTERNAL_OES);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture);

        int position = GLES20.glGetAttribLocation(program, "position");
        GLES20.glEnableVertexAttribArray(position);
        GLES20.glVertexAttribPointer(position, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, vertexBuffer);

        int textureCoordinate = GLES20.glGetAttribLocation(program, "inputTextureCoordinate");
        GLES20.glEnableVertexAttribArray(textureCoordinate);
        GLES20.glVertexAttribPointer(textureCoordinate, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, textureVerticesBuffer);

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                GLES20.GL_UNSIGNED_SHORT, drawListBuffer);

        // Disable vertex array
        GLES20.glDisableVertexAttribArray(position);
        GLES20.glDisableVertexAttribArray(textureCoordinate);
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        GLES20.glClearColor(0.5f, 0.5f, 0.5f, 1f);

        int vertexShader = loadGLShader(GLES20.GL_VERTEX_SHADER, "shader/vertex.glsl");
        int fragmentShader = loadGLShader(GLES20.GL_FRAGMENT_SHADER, "shader/fragment.glsl");

        program = GLES20.glCreateProgram();
        GLES20.glAttachShader(program, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(program, fragmentShader); // add the fragment shader to program
        GLES20.glLinkProgram(program);

        final int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0);

        if (linkStatus[0] == 0) {
            Log.e("On Surface Created", "Error linking program: " + GLES20.glGetProgramInfoLog(program));
            GLES20.glDeleteProgram(program);
            throw new RuntimeException("Error linking program");
        }

        //todo: double check why length * 4?!
        ByteBuffer bb = ByteBuffer.allocateDirect(SQUARE_VERTICES.length * 4);
        bb.order(ByteOrder.nativeOrder());
        vertexBuffer = bb.asFloatBuffer();
        vertexBuffer.put(SQUARE_VERTICES);
        vertexBuffer.position(0);

        ByteBuffer bb2 = ByteBuffer.allocateDirect(TEXTURE_VERTICES.length * 4);
        bb2.order(ByteOrder.nativeOrder());
        textureVerticesBuffer = bb2.asFloatBuffer();
        textureVerticesBuffer.put(TEXTURE_VERTICES);
        textureVerticesBuffer.position(0);

        ByteBuffer dlb = ByteBuffer.allocateDirect(drawOrder.length * 2);
        dlb.order(ByteOrder.nativeOrder());
        drawListBuffer = dlb.asShortBuffer();
        drawListBuffer.put(drawOrder);
        drawListBuffer.position(0);

        texture = createTexture();
        startCamera(texture);
    }

    @Override
    public void onRendererShutdown() {

    }

    private int loadGLShader(int type, String pathToSource) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, ShaderCodeLoader.readSourceFile(pathToSource, getAssets()));
        GLES20.glCompileShader(shader);

        // Get the compilation status.
        final int[] compileStatus = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0);

        // If the compilation failed, delete the shader.
        if (compileStatus[0] == 0) {
            Log.e("loadGLShader", "Error compiling shader: " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Error creating shader.");
        }

        return shader;
    }

    private int createTexture() {
        int[] texture = new int[1];

        GLES20.glGenTextures(1, texture, 0);
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texture[0]);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameterf(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

        return texture[0];
    }

    public void startCamera(int texture) {
        surface = new SurfaceTexture(texture);
        surface.setOnFrameAvailableListener(this);

        //todo: use new camera2 package stuff.
        Camera camera = Camera.open();

        try {
            camera.setPreviewTexture(surface);
            camera.startPreview();
        } catch (IOException ioe) {
            Log.e("startCamera", "Camera launch failed.");
            throw new RuntimeException("Camera launch failed.");
        }
    }

    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        cardboardView.requestRender();
    }
}
