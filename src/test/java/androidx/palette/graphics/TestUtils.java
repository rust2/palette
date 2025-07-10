package androidx.palette.graphics;

import android.graphics.Bitmap;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;

import static androidx.core.graphics.ColorUtils.blue;
import static androidx.core.graphics.ColorUtils.green;
import static androidx.core.graphics.ColorUtils.red;
import static org.junit.Assert.assertEquals;

class TestUtils {
    static Bitmap loadSampleBitmap() {
        return Bitmap.of(new Pixmap(Gdx.files.internal("photo.jpg")));
    }

    static void assertCloseColors(int expected, int actual) {
        assertEquals(red(expected), red(actual), 8);
        assertEquals(green(expected), green(actual), 8);
        assertEquals(blue(expected), blue(actual), 8);
    }
}