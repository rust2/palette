package android.graphics;

import androidx.core.graphics.ColorUtils;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.GdxRuntimeException;
import org.jetbrains.annotations.NotNull;

/**
 * This class just connects libGDX images with android palette implementation.
 * Here are only required methods for palette to work
 */
public class Bitmap implements Disposable, AutoCloseable {
    private Pixmap pixmap;

    // todo: I'm not sure if caller can dispose Texture right after creating Bitmap
    private Bitmap(Texture t) {
        t.getTextureData().prepare();
        pixmap = t.getTextureData().consumePixmap();
    }

    private Bitmap(Pixmap p) {
        pixmap = p;
    }

    public static Bitmap of(Texture t) {
        return new Bitmap(t);
    }

    /** Returns new Bitmap initialized with {@link Pixmap p}. New bitmap will share pixmap instance */
    public static Bitmap of(Pixmap p) {
        return new Bitmap(p);
    }

    /** Returns new Bitmap initialized with copy of {@link Pixmap p}. You will have to dispose bitmap and pixmap separately */
    public static Bitmap ofPixmapCopy(Pixmap p) {
        Pixmap p1 = new Pixmap(p.getWidth(), p.getHeight(), p.getFormat());
        p1.drawPixmap(p, 0, 0);
        return new Bitmap(p1);
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
     * @param pixels The array to receive the bitmap's colors
     * @param offset The first index to write into pixels[]
     * @param stride The number of entries in pixels[] to skip between rows
     * @param x      The x coordinate of the first pixel to read from the bitmap
     * @param y      The y coordinate of the first pixel to read from the bitmap
     * @param width  The number of pixels to read from each row
     * @param height The number of rows to read
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

    /**
     * Shared code to check for illegal arguments passed to getPixels()
     * or setPixels()
     *
     * @param x left edge of the area of pixels to access
     * @param y top edge of the area of pixels to access
     * @param width width of the area of pixels to access
     * @param height height of the area of pixels to access
     * @param offset offset into pixels[] array
     * @param stride number of elements in pixels[] between each logical row
     * @param pixels array to hold the area of pixels being accessed
     */
    private void checkPixelsAccess(int x, int y, int width, int height, int offset, int stride, int pixels[]) {
        checkXYSign(x, y);
        if (width < 0) {
            throw new IllegalArgumentException("width must be >= 0");
        }
        if (height < 0) {
            throw new IllegalArgumentException("height must be >= 0");
        }
        if (x + width > getWidth()) {
            throw new IllegalArgumentException("x + width must be <= bitmap.width()");
        }
        if (y + height > getHeight()) {
            throw new IllegalArgumentException("y + height must be <= bitmap.height()");
        }
        if (Math.abs(stride) < width) {
            throw new IllegalArgumentException("abs(stride) must be >= width");
        }
        int lastScanline = offset + (height - 1) * stride;
        int length = pixels.length;
        if (offset < 0 || (offset + width > length)
                || lastScanline < 0
                || (lastScanline + width > length)) {
            throw new ArrayIndexOutOfBoundsException();
        }
    }

    /**
     * Common code for checking that x and y are >= 0
     *
     * @param x x coordinate to ensure is >= 0
     * @param y y coordinate to ensure is >= 0
     */
    private static void checkXYSign(int x, int y) {
        if (x < 0) {
            throw new IllegalArgumentException("x must be >= 0");
        }
        if (y < 0) {
            throw new IllegalArgumentException("y must be >= 0");
        }
    }

    public static Bitmap createScaledBitmap(Bitmap bitmap, int newWidth, int newHeight, boolean bilinearFilter) {
        Pixmap p = new Pixmap(newWidth, newHeight, bitmap.pixmap.getFormat());
        if (bilinearFilter) p.setFilter(Pixmap.Filter.BiLinear);
        p.drawPixmap(bitmap.pixmap,
                     0, 0, bitmap.pixmap.getWidth(), bitmap.pixmap.getHeight(),
                     0, 0, newWidth, newHeight
        );
        return new Bitmap(p);
    }

    @SuppressWarnings("all")
    private void checkRecycled(String message) {
        if (pixmap.isDisposed())
            throw new GdxRuntimeException(message);
    }

    @Override
    public void close() {
        dispose();
    }
}