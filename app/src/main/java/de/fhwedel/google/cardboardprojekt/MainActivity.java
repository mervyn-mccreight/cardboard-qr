package de.fhwedel.google.cardboardprojekt;

import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.opengl.GLES20;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

import javax.microedition.khronos.egl.EGLConfig;

import de.fhwedel.google.cardboardprojekt.gl.ShaderCodeLoader;


public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer, ImageReader.OnImageAvailableListener {

    private long id = 0;

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

    private FloatBuffer vertexBuffer;
    private FloatBuffer textureVerticesBuffer;
    private SurfaceTexture surface;
    private int program;
    private int texture;
    private ShortBuffer drawListBuffer;
    private Handler cameraHandler;
    private CameraStateCallback cameraStateCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Associate a CardboardView.StereoRenderer with cardboardView.
        CardboardView cardboardView = (CardboardView) findViewById(R.id.cardboard_view);
        cardboardView.setRenderer(this);
        // Associate the cardboardView with this activity.
        setCardboardView(cardboardView);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
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

        try {
            startCamera(texture);
        } catch (CameraAccessException e) {
            throw new RuntimeException(e);
        }
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

    public void startCamera(final int texture) throws CameraAccessException {
        surface = new SurfaceTexture(texture);

        CameraManager cameraManager = (CameraManager) getApplicationContext().getSystemService(CAMERA_SERVICE);
        String backFacingCameraId = getBackFacingCameraId(cameraManager);
        setSurfaceTextureSize(cameraManager, backFacingCameraId, surface);

        StreamConfigurationMap streamConfigurationMap = getStreamConfigurationMap(cameraManager, backFacingCameraId);
        Size[] outputSizes = streamConfigurationMap.getOutputSizes(ImageFormat.YUV_420_888);

        //todo: very nexus 5 specific :D - 6 --> 1280 x 9xx (?)
        ImageReader imageReader = ImageReader.newInstance(outputSizes[6].getWidth(), outputSizes[6].getHeight(), ImageFormat.YUV_420_888, 5);
        imageReader.setOnImageAvailableListener(this, startBackgroundThread());

        cameraHandler = startBackgroundThread();
        cameraStateCallback = new CameraStateCallback(surface, imageReader);

        cameraManager.openCamera(backFacingCameraId, cameraStateCallback, cameraHandler);
    }

    private void setSurfaceTextureSize(CameraManager cameraManager, String backFacingCameraId, SurfaceTexture surfaceTexture) throws CameraAccessException {
        StreamConfigurationMap streamConfigurationMap = getStreamConfigurationMap(cameraManager, backFacingCameraId);
        Size maxSize = getMaxSize(streamConfigurationMap.getOutputSizes(SurfaceTexture.class));
        surfaceTexture.setDefaultBufferSize(maxSize.getWidth(), maxSize.getHeight());
    }

    private StreamConfigurationMap getStreamConfigurationMap(CameraManager cameraManager, String backFacingCameraId) throws CameraAccessException {
        CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(backFacingCameraId);
        return cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
    }

    private Size getMaxSize(Size[] sizes) {
        return Collections.max(Arrays.asList(sizes), new Comparator<Size>() {
            @Override
            public int compare(Size lhs, Size rhs) {
                int leftArea = lhs.getHeight() * lhs.getWidth();
                int rightArea = rhs.getWidth() * rhs.getHeight();

                return leftArea - rightArea;
            }
        });
    }

    private String getBackFacingCameraId(CameraManager cameraManager) throws CameraAccessException {
        String[] cameraIdList = cameraManager.getCameraIdList();

        for (String cameraId : cameraIdList) {
            CameraCharacteristics cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId);
            if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK) {
                return cameraId;
            }
        }

        Log.e("getBackFacingCameraId", "No back-facing camera found.");
        throw new IllegalStateException("No back-facing camera found.");
    }

    /**
     * Starts a background thread and its {@link Handler}.
     */
    private Handler startBackgroundThread() {
        HandlerThread thread = new HandlerThread(String.valueOf(id++));
        thread.start();
        return new Handler(thread.getLooper());
    }

    /**
     * Stops the background thread and its {@link Handler}.
     */
    private void stopBackgroundThread(Handler handler) {
        HandlerThread thread = (HandlerThread) handler.getLooper().getThread();
        thread.quitSafely();
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        if (cameraStateCallback != null) {
            cameraStateCallback.close();
            stopBackgroundThread(cameraHandler);
        }
        super.onPause();
    }

    @Override
    public void onImageAvailable(ImageReader reader) {
        Image image = reader.acquireLatestImage();

        if (image != null) {
            byte[] dataFromImage = ImageFoo.getDataFromImage(image);
            PlanarYUVLuminanceSource planarYUVLuminanceSource = new PlanarYUVLuminanceSource(dataFromImage, image.getWidth(), image.getHeight(), 0, 0, image.getWidth(), image.getHeight(), false);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(planarYUVLuminanceSource));

            QRCodeReader qrCodeReader = new QRCodeReader();
            try {
                Result decode = qrCodeReader.decode(bitmap);
                ResultPoint[] resultPoints = decode.getResultPoints();

                String coordinate = "";

                int i = 0;

                for (ResultPoint resultPoint : resultPoints) {
                    coordinate += String.format("[%d]: x %f, y %f; ", i++, resultPoint.getX(), resultPoint.getY());
                }


                Log.d("QRCodeReader", decode.getText() + " " + coordinate);
            } catch (NotFoundException | ChecksumException | FormatException e) {
//                Log.d("QRCodeReader", "NotFoundException");
            }

            image.close();
        }
    }
}
