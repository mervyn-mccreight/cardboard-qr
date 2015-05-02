package de.fhwedel.google.cardboardprojekt;

import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CaptureFailure;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.media.ImageReader;
import android.util.Log;
import android.view.Surface;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;

import java.util.List;

import static android.hardware.camera2.CameraCaptureSession.CaptureCallback;
import static android.hardware.camera2.CameraDevice.TEMPLATE_RECORD;

public class CameraStateCallback extends CameraDevice.StateCallback {

    private SurfaceTexture texture;
    private final ImageReader imageReader;

    private Optional<CameraDevice> device = Optional.absent();
    private Optional<CameraCaptureSession> session = Optional.absent();

    public CameraStateCallback(SurfaceTexture texture, ImageReader imageReader) {
        this.texture = texture;
        this.imageReader = imageReader;
    }

    private CaptureRequest createCaptureRequest(CameraDevice device, List<Surface> targets) throws CameraAccessException {
        CaptureRequest.Builder builder = device.createCaptureRequest(TEMPLATE_RECORD);

        for (Surface target : targets) {
            builder.addTarget(target);
        }

        return builder.build();
    }

    //todo: apparently does not work.
    public void close() {
        if (session.isPresent()) {
            try {
                session.get().stopRepeating();
            } catch (CameraAccessException e) {
                throw new RuntimeException(e);
            }
            session.get().close();
            session = Optional.absent();
        }

        if (device.isPresent()) {
            device.get().close();
            device = Optional.absent();
        }
    }

    @Override
    public void onOpened(CameraDevice camera) {
        try {
            final List<Surface> targets = Lists.newArrayList(new Surface(texture), imageReader.getSurface());

            device = Optional.of(camera);

            camera.createCaptureSession(targets, new CameraCaptureSession.StateCallback() {
                        @Override
                        public void onConfigured(CameraCaptureSession session) {
                            try {
                                CaptureRequest captureRequest = createCaptureRequest(session.getDevice(), targets);
                                session.setRepeatingRequest(captureRequest, new Foo(), null);
                            } catch (CameraAccessException e) {
                                Log.e("CameraStateCallback", "Something went wrong deeper.");
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public void onConfigureFailed(CameraCaptureSession session) {
                            try {
                                session.abortCaptures();
                            } catch (CameraAccessException e) {
                                Log.e("CameraStateCallback", "Something went wrong when it went wrong.");
                                throw new RuntimeException(e);
                            }
                            session.close();
                        }
                    },
                    null);
        } catch (CameraAccessException e) {
            Log.e("CameraStateCallback", "Something went wrong.");
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onDisconnected(CameraDevice camera) {
        Log.d("CameraStateCallback", "onDisconnected.");
        device = Optional.absent();
        camera.close();
    }

    @Override
    public void onError(CameraDevice camera, int error) {
        Log.e("CameraStateCallback", "onError with code " + error);
        device = Optional.absent();
        camera.close();
    }

    private class Foo extends CaptureCallback {
        @Override
        public void onCaptureStarted(CameraCaptureSession session, CaptureRequest request, long timestamp, long frameNumber) {
            Log.d("Foo", "onCaptureStarted");
        }

        @Override
        public void onCaptureProgressed(CameraCaptureSession session, CaptureRequest request, CaptureResult partialResult) {
            Log.d("Foo", "onCaptureProgressed");
        }

        @Override
        public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request, TotalCaptureResult result) {
            Log.d("Foo", "onCaptureCompleted");
        }

        @Override
        public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
            Log.d("Foo", "onCaptureSequenceCompleted");
        }

        @Override
        public void onCaptureSequenceAborted(CameraCaptureSession session, int sequenceId) {
            Log.d("Foo", "onCaptureSequenceAborted");
        }

        @Override
        public void onCaptureFailed(CameraCaptureSession session, CaptureRequest request, CaptureFailure failure) {
            Log.d("Foo", "Capture Failed.");
        }
    }
}
