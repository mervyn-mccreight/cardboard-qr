package de.fhwedel.google.cardboardprojekt;

import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import com.google.common.base.Preconditions;

import java.nio.ByteBuffer;

public class ImageFoo {
    private static final String TAG = "ImageFoo";

    /**
     * <p>Check android image format validity for an image, only support below formats:</p>
     * <p/>
     * <p>Valid formats are YUV_420_888/NV21/YV12 for video decoder</p>
     */
    private static void checkAndroidImageFormat(Image image) {
        int format = image.getFormat();
        Image.Plane[] planes = image.getPlanes();
        switch (format) {
            case ImageFormat.YUV_420_888:
            case ImageFormat.NV21:
            case ImageFormat.YV12:
                if (planes.length != 3) {
                    throw new RuntimeException("YUV420 format Images should have 3 planes");
                }
                break;
            default:
                throw new RuntimeException("Unsupported Format");
        }
    }

    /**
     * Get a byte array image data from an Image object.
     * <p>
     * Read data from all planes of an Image into a contiguous unpadded,
     * unpacked 1-D linear byte array, such that it can be write into disk, or
     * accessed by software conveniently. It supports YUV_420_888/NV21/YV12
     * input Image format.
     * </p>
     * <p>
     * For YUV_420_888/NV21/YV12/Y8/Y16, it returns a byte array that contains
     * the Y plane data first, followed by U(Cb), V(Cr) planes if there is any
     * (xstride = width, ystride = height for chroma and luma components).
     * </p>
     */
    public static byte[] getDataFromImage(Image image) {
        int format = image.getFormat();
        int width = image.getWidth();
        int height = image.getHeight();
        int rowStride, pixelStride;
        byte[] data = null;
        // Read image data
        Image.Plane[] planes = image.getPlanes();
        Preconditions.checkArgument(planes != null && planes.length > 0, "Fail to get image planes");

        // Check image validity
        checkAndroidImageFormat(image);

        ByteBuffer buffer = null;
        int offset = 0;
        data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];
        Log.d(TAG, "get data from " + planes.length + " planes");
        for (int i = 0; i < planes.length; i++) {
            buffer = planes[i].getBuffer();

            if (buffer == null) {
                throw new RuntimeException("Fail to get bytebuffer from plane");
            }

            rowStride = planes[i].getRowStride();
            pixelStride = planes[i].getPixelStride();

            if (pixelStride <= 0) {
                throw new RuntimeException("pixel stride " + pixelStride + " is invalid");
            }

            Log.d(TAG, "pixelStride " + pixelStride);
            Log.d(TAG, "rowStride " + rowStride);
            Log.d(TAG, "width " + width);
            Log.d(TAG, "height " + height);

            // For multi-planar yuv images, assuming yuv420 with 2x2 chroma subsampling.
            int w = (i == 0) ? width : width / 2;
            int h = (i == 0) ? height : height / 2;

            if (rowStride < w) {
                throw new RuntimeException("rowStride " + rowStride + " should be >= width " + w);
            }

            for (int row = 0; row < h; row++) {
                int bytesPerPixel = ImageFormat.getBitsPerPixel(format) / 8;
                if (pixelStride == bytesPerPixel) {
                    // Special case: optimized read of the entire row
                    int length = w * bytesPerPixel;
                    buffer.get(data, offset, length);
                    // Advance buffer the remainder of the row stride
                    buffer.position(buffer.position() + rowStride - length);
                    offset += length;
                } else {
                    // Generic case: should work for any pixelStride but slower.
                    // Use intermediate buffer to avoid read byte-by-byte from
                    // DirectByteBuffer, which is very bad for performance
                    buffer.get(rowData, 0, rowStride);
                    for (int col = 0; col < w; col++) {
                        data[offset++] = rowData[col * pixelStride];
                    }
                }
            }

            Log.d(TAG, "Finished reading data from plane " + i);
        }
        return data;
    }
}
