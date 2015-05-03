package de.fhwedel.google.cardboardprojekt;

import android.media.Image;

import java.nio.ByteBuffer;

public class ImageFoo {

    //todo: performance is very much lol.
    public static byte[] getDataFromImage(Image image) {
        Image.Plane[] planes = image.getPlanes();
        int height = image.getHeight();
        int width = image.getWidth();


        Image.Plane planeY = planes[0];
        Image.Plane planeU = planes[1];
        Image.Plane planeV = planes[2];

        ByteBuffer bufferY = planeY.getBuffer();
        ByteBuffer bufferU = planeU.getBuffer();
        ByteBuffer bufferV = planeV.getBuffer();

        byte[] y = new byte[width * height * planeY.getPixelStride()];
        byte[] u = new byte[(width / 2 * height / 2) * planeU.getPixelStride() - 1];
        byte[] v = new byte[(width / 2 * height / 2) * planeV.getPixelStride() - 1];

        bufferY.get(y);
        bufferU.get(u);
        bufferV.get(v);

        byte[] bytes = new byte[y.length + u.length + v.length];

        int offset = 0;

        //todo: lol.
        for (byte b : y) {
            bytes[offset++] = b;
        }

        for (byte b : u) {
            bytes[offset++] = b;
        }

        for (byte b : v) {
            bytes[offset++] = b;
        }

        return bytes;
    }
}
