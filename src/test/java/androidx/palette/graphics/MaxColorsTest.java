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

import static org.junit.Assert.assertTrue;

import android.graphics.Bitmap;
import kww.test.GdxTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(GdxTestRunner.class)
public class MaxColorsTest {

    @Test
    public void testMaxColorCount32() {
        testMaxColorCount(32);
    }

    @Test
    public void testMaxColorCount1() {
        testMaxColorCount(1);
    }

    @Test
    public void testMaxColorCount15() {
        testMaxColorCount(15);
    }

    private void testMaxColorCount(int colorCount) {
        try (Bitmap sample = TestUtils.loadSampleBitmap()) {
            Palette newPalette = Palette.from(sample)
                    .maximumColorCount(colorCount)
                    .generate();
            assertTrue(newPalette.getSwatches().size() <= colorCount);
        }
    }
}