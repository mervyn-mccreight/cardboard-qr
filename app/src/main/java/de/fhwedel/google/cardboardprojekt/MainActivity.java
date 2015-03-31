package de.fhwedel.google.cardboardprojekt;

import android.os.Bundle;

import com.google.vrtoolkit.cardboard.CardboardActivity;
import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import javax.microedition.khronos.egl.EGLConfig;


public class MainActivity extends CardboardActivity implements CardboardView.StereoRenderer {

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

    }

    @Override
    public void onDrawEye(Eye eye) {

    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {

    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {

    }

    @Override
    public void onRendererShutdown() {

    }
}
