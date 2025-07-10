/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.palette.graphics;

import static androidx.core.graphics.ColorUtils.argbToRgba;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.graphics.Bitmap;
import android.graphics.Color;

import com.badlogic.gdx.graphics.Pixmap;
import kww.test.GdxTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(GdxTestRunner.class)
public class BucketTests {
    @Test
    public void testSourceBitmapNotRecycled() {
        try (Bitmap sample = TestUtils.loadSampleBitmap()) {
            Palette.from(sample).generate();
            assertFalse(sample.isRecycled());
        }
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testSwatchesUnmodifiable() {
        try (Bitmap sample = TestUtils.loadSampleBitmap()) {
            Palette p = Palette.from(sample).generate();
            p.getSwatches().remove(0);
        }
    }

    @Test
    public void testSwatchesBuilder() {
        ArrayList<Palette.Swatch> swatches = new ArrayList<>();
        swatches.add(new Palette.Swatch(Color.BLACK, 40));
        swatches.add(new Palette.Swatch(Color.GREEN, 60));
        swatches.add(new Palette.Swatch(Color.BLUE, 10));

        Palette p = Palette.from(swatches);

        assertEquals(swatches, p.getSwatches());
    }

    @Test
    public void testRegionWhole() {
        try (Bitmap sample = TestUtils.loadSampleBitmap()) {
            Palette.Builder b = new Palette.Builder(sample);
            b.setRegion(0, 0, sample.getWidth(), sample.getHeight());
            b.generate();
        }
    }

    @Test
    public void testRegionUpperLeft() {
        try (Bitmap sample = TestUtils.loadSampleBitmap()) {
            Palette.Builder b = new Palette.Builder(sample);
            b.setRegion(0, 0, sample.getWidth() / 2, sample.getHeight() / 2);
            b.generate();
        }
    }

    @Test
    public void testRegionBottomRight() {
        try (Bitmap sample = TestUtils.loadSampleBitmap()) {
            Palette.Builder b = new Palette.Builder(sample);
            b.setRegion(sample.getWidth() / 2, sample.getHeight() / 2,
                        sample.getWidth(), sample.getHeight());
            b.generate();
        }
    }

    @Test
    public void testOnePixelTallBitmap() {
//        final Bitmap bitmap = Bitmap.createBitmap(1000, 1, Bitmap.Config.ARGB_8888);
        try (Bitmap bitmap = Bitmap.of(new Pixmap(1000, 1, Pixmap.Format.RGBA8888))) {
            Palette.Builder b = new Palette.Builder(bitmap);
            b.generate();
        }
    }

    @Test
    public void testOnePixelWideBitmap() {
//        final Bitmap bitmap = Bitmap.createBitmap(1, 1000, Bitmap.Config.ARGB_8888);
        try (Bitmap bitmap = Bitmap.of(new Pixmap(1, 1000, Pixmap.Format.RGBA8888))) {
            Palette.Builder b = new Palette.Builder(bitmap);
            b.generate();
        }
    }

    @Test
    public void testBlueBitmapReturnsBlueSwatch() {
//        final Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawColor(Color.BLUE);

        try (Bitmap bitmap = Bitmap.of(
                new Pixmap(300, 300, Pixmap.Format.RGBA8888) {{
                    setColor(argbToRgba(Color.BLUE));
                    fill();
                }}
        )) {
            final Palette palette = Palette.from(bitmap).generate();

            assertEquals(1, palette.getSwatches().size());

            final Palette.Swatch swatch = palette.getSwatches().get(0);
            TestUtils.assertCloseColors(Color.BLUE, swatch.getRgb());
        }
    }

    @Test
    public void testBlueBitmapWithRegionReturnsBlueSwatch() {
//        final Bitmap bitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888);
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawColor(Color.BLUE);

        try (Bitmap bitmap = Bitmap.of(
                new Pixmap(300, 300, Pixmap.Format.RGBA8888) {{
                    setColor(argbToRgba(Color.BLUE));
                    fill();
                }}
        )) {
            final Palette palette = Palette.from(bitmap)
                    .setRegion(0, bitmap.getHeight() / 2, bitmap.getWidth(), bitmap.getHeight())
                    .generate();

            assertEquals(1, palette.getSwatches().size());

            final Palette.Swatch swatch = palette.getSwatches().get(0);
            TestUtils.assertCloseColors(Color.BLUE, swatch.getRgb());
        }
    }

    @Test
    public void testDominantSwatch() {
//        final Bitmap bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);
//
//        // First fill the canvas with blue
//        Canvas canvas = new Canvas(bitmap);
//        canvas.drawColor(Color.BLUE);
//
//        final Paint paint = new Paint();
//        // Now we'll draw the top 10px tall rect with green
//        paint.setColor(Color.GREEN);
//        canvas.drawRect(0, 0, 100, 10, paint);
//
//        // Now we'll draw the next 20px tall rect with red
//        paint.setColor(Color.RED);
//        canvas.drawRect(0, 11, 100, 30, paint);

        try (Bitmap bitmap = Bitmap.of(
                new Pixmap(100, 100, Pixmap.Format.RGBA8888) {{
                    // First fill the canvas with blue
                    setColor(argbToRgba(Color.BLUE));
                    fill();

                    // Now we'll draw the top 10px tall rect with green
                    setColor(argbToRgba(Color.GREEN));
                    drawRectangle(0, 0, 100, 10);

                    // Now we'll draw the next 20px tall rect with red
                    setColor(argbToRgba(Color.RED));
                    drawRectangle(0, 10, 100, 20);
                }}
        )) {
            // Now generate a palette from the bitmap
            final Palette palette = Palette.from(bitmap).generate();

            // First assert that there are 3 swatches
            assertEquals(3, palette.getSwatches().size());

            // Now assert that the dominant swatch is blue
            final Palette.Swatch swatch = palette.getDominantSwatch();
            assertNotNull(swatch);
            TestUtils.assertCloseColors(Color.BLUE, swatch.getRgb());
        }
    }

}