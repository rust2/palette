package android.graphics;

import androidx.core.graphics.ColorUtils;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jetbrains.annotations.NotNull;

public class Bitmap implements Disposable {
    private Pixmap pixmap;

    public Bitmap(Texture t) {
        t.getTextureData().prepare();
        pixmap = t.getTextureData().consumePixmap();
    }

    public Bitmap(Pixmap p) {
        pixmap = p;
    }

    public int getWidth() {
        return pixmap.getWidth();
    }

    public int getHeight() {
        return pixmap.getHeight();
    }

    //region Disposable
    public void recycle() {
        dispose();
    }

    public boolean isRecycled() {
        return pixmap.isDisposed();
    }

    @Override
    public void dispose() {
        if (!pixmap.isDisposed())
            pixmap.dispose();
    }
    //endregion Disposable

    /**
     * Returns in pixels[] a copy of the data in the bitmap. Each value is
     * a packed int representing a non-premultiplied ARGB color in sRGB.
     *
     * @param pixels   The array to receive the bitmap's colors
     * @param offset   The first index to write into pixels[]
     * @param stride   The number of entries in pixels[] to skip between rows
     * @param x        The x coordinate of the first pixel to read from the bitmap
     * @param y        The y coordinate of the first pixel to read from the bitmap
     * @param width    The number of pixels to read from each row
     * @param height   The number of rows to read
     */
    public void getPixels(int @NotNull [] pixels, int offset, int stride, int x, int y, int width, int height) {
        checkRecycled("Can't call getPixels() on a recycled bitmap");

        if (width == 0 || height == 0) {
            return; // nothing to do
        }

        checkPixelsAccess(x, y, width, height, offset, stride, pixels);

        // Read the pixels row by row
        for (int row = 0; row < height; row++) {
            int srcY = y + row;
            int dstIndex = offset + row * stride;

            for (int col = 0; col < width; col++) {
                int srcX = x + col;
                int pixel = ColorUtils.rgbaToArgb(pixmap.getPixel(srcX, srcY)); // convert from Pixmap's RGBA to android's ARGB
                pixels[dstIndex + col] = pixel;
            }
        }
    }

    private void checkPixelsAccess(int x, int y, int width, int height,
                                   int offset, int stride, int[] pixels) {
        if (x < 0 || y < 0 || width <= 0 || height <= 0 ||
                x + width > getWidth() || y + height > getHeight()) {
            throw new IllegalArgumentException("x or y or width or height exceed bitmap bounds.");
        }

        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }

        int requiredSize = offset + ((height - 1) * Math.abs(stride)) + width;
        if (pixels.length < requiredSize) {
            throw new ArrayIndexOutOfBoundsException("pixels array is too small (" + pixels.length + " < " + requiredSize + ")");
        }
    }

    public static Bitmap createScaledBitmap(Bitmap bitmap, int newWidth, int newHeight, boolean bilinearFilter) {
        Pixmap p = new Pixmap(newWidth, newHeight, bitmap.pixmap.getFormat());
        if(bilinearFilter) p.setFilter(Pixmap.Filter.BiLinear);
        p.drawPixmap(bitmap.pixmap,
                            0, 0, bitmap.pixmap.getWidth(), bitmap.pixmap.getHeight(),
                            0, 0, newWidth, newHeight
        );
        return new Bitmap(p);
    }

    @SuppressWarnings("all")
    private void checkRecycled(String message) {
        if(pixmap.isDisposed())
            throw new GdxRuntimeException(message);
    }
}