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

import android.graphics.Bitmap;
import android.util.SparseBooleanArray;
import androidx.annotation.ColorInt;
import androidx.core.graphics.ColorUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.ObjectMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A helper class to extract prominent colors from an image.
 * <p>
 * A number of colors with different profiles are extracted from the image:
 * <ul>
 *     <li>Vibrant</li>
 *     <li>Vibrant Dark</li>
 *     <li>Vibrant Light</li>
 *     <li>Muted</li>
 *     <li>Muted Dark</li>
 *     <li>Muted Light</li>
 * </ul>
 * These can be retrieved from the appropriate getter method.
 *
 * <p>
 * Instances are created with a {@link Builder} which supports several options to tweak the
 * generated Palette. See that class' documentation for more information.
 * <p>
 * Generation should always be completed on a background thread, ideally the one in
 * which you load your image on. {@link Builder} supports both synchronous and asynchronous
 * generation:
 *
 * <pre>
 * // Synchronous
 * Palette p = Palette.from(bitmap).generate();
 *
 * // Asynchronous <b>(not ported yet.)</b>
 * Palette.from(bitmap).generate(new PaletteAsyncListener() {
 *     public void onGenerated(Palette p) {
 *         // Use generated instance
 *     }
 * });
 * </pre>
 */
public final class Palette {

    // todo: implement async load
//    /**
//     * Listener to be used with {@link #generateAsync(Bitmap, PaletteAsyncListener)} or
//     * {@link #generateAsync(Bitmap, int, PaletteAsyncListener)}
//     */
//    public interface PaletteAsyncListener {
//
//        /**
//         * Called when the {@link Palette} has been generated. {@code null} will be passed when an
//         * error occurred during generation.
//         */
//        void onGenerated(@Nullable Palette palette);
//    }

    static final int DEFAULT_RESIZE_BITMAP_AREA = 112 * 112;
    static final int DEFAULT_CALCULATE_NUMBER_COLORS = 16;

    static final float MIN_CONTRAST_TITLE_TEXT = 3.0f;
    static final float MIN_CONTRAST_BODY_TEXT = 4.5f;

    static final String LOG_TAG = "Palette";
    static final boolean LOG_TIMINGS = false;

    /**
     * Start generating a {@link Palette} with the returned {@link Builder} instance.
     */
    public static @NotNull Builder from(@NotNull Bitmap bitmap) {
        return new Builder(bitmap);
    }

    /**
     * Generate a {@link Palette} from the pre-generated list of {@link Palette.Swatch} swatches.
     * This is useful for testing, or if you want to resurrect a {@link Palette} instance from a
     * list of swatches. Will return null if the {@code swatches} is null.
     */
    public static @NotNull Palette from(@NotNull List<Swatch> swatches) {
        return new Builder(swatches).generate();
    }

    /**
     * @deprecated Use {@link Builder} to generate the Palette.
     */
    @Deprecated
    public static @NotNull Palette generate(@NotNull Bitmap bitmap) {
        return from(bitmap).generate();
    }

    /**
     * @deprecated Use {@link Builder} to generate the Palette.
     */
    @Deprecated
    public static @NotNull Palette generate(@NotNull Bitmap bitmap, int numColors) {
        return from(bitmap).maximumColorCount(numColors).generate();
    }

    // todo: implement async load
//    /**
//     * @deprecated Use {@link Builder} to generate the Palette.
//     */
//    @Deprecated
//    public static @NotNull AsyncTask<Bitmap, Void, Palette> generateAsync(@NotNull Bitmap bitmap, @NotNull PaletteAsyncListener listener) {
//        return from(bitmap).generate(listener);
//    }

//    /**
//     * @deprecated Use {@link Builder} to generate the Palette.
//     */
//    @Deprecated
//    public static @NotNull AsyncTask<Bitmap, Void, Palette> generateAsync(@NotNull Bitmap bitmap, int numColors, @NotNull PaletteAsyncListener listener) {
//        return from(bitmap).maximumColorCount(numColors).generate(listener);
//    }

    private final List<Swatch> mSwatches;
    private final List<Target> mTargets;

    private final ObjectMap<Target, Swatch> mSelectedSwatches;
    private final SparseBooleanArray mUsedColors;

    private final @Nullable Swatch mDominantSwatch;

    Palette(List<Swatch> swatches, List<Target> targets) {
        mSwatches = swatches;
        mTargets = targets;

        mUsedColors = new SparseBooleanArray();
        mSelectedSwatches = new ObjectMap<>();

        mDominantSwatch = findDominantSwatch();
    }

    /**
     * Returns all of the swatches which make up the palette.
     */
    public @NotNull List<Swatch> getSwatches() {
        return Collections.unmodifiableList(mSwatches);
    }

    /**
     * Returns the targets used to generate this palette.
     */
    public @NotNull List<Target> getTargets() {
        return Collections.unmodifiableList(mTargets);
    }

    /**
     * Returns the most vibrant swatch in the palette. Might be null.
     *
     * @see Target#VIBRANT
     */
    public @Nullable Swatch getVibrantSwatch() {
        return getSwatchForTarget(Target.VIBRANT);
    }

    /**
     * Returns a light and vibrant swatch from the palette. Might be null.
     *
     * @see Target#LIGHT_VIBRANT
     */
    public @Nullable Swatch getLightVibrantSwatch() {
        return getSwatchForTarget(Target.LIGHT_VIBRANT);
    }

    /**
     * Returns a dark and vibrant swatch from the palette. Might be null.
     *
     * @see Target#DARK_VIBRANT
     */
    public @Nullable Swatch getDarkVibrantSwatch() {
        return getSwatchForTarget(Target.DARK_VIBRANT);
    }

    /**
     * Returns a muted swatch from the palette. Might be null.
     *
     * @see Target#MUTED
     */
    public @Nullable Swatch getMutedSwatch() {
        return getSwatchForTarget(Target.MUTED);
    }

    /**
     * Returns a muted and light swatch from the palette. Might be null.
     *
     * @see Target#LIGHT_MUTED
     */
    public @Nullable Swatch getLightMutedSwatch() {
        return getSwatchForTarget(Target.LIGHT_MUTED);
    }

    /**
     * Returns a muted and dark swatch from the palette. Might be null.
     *
     * @see Target#DARK_MUTED
     */
    public @Nullable Swatch getDarkMutedSwatch() {
        return getSwatchForTarget(Target.DARK_MUTED);
    }

    /**
     * Returns the most vibrant color in the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getVibrantSwatch()
     */
    @ColorInt
    public int getVibrantColor(@ColorInt final int defaultColor) {
        return getColorForTarget(Target.VIBRANT, defaultColor);
    }

    /**
     * Returns a light and vibrant color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getLightVibrantSwatch()
     */
    @ColorInt
    public int getLightVibrantColor(@ColorInt final int defaultColor) {
        return getColorForTarget(Target.LIGHT_VIBRANT, defaultColor);
    }

    /**
     * Returns a dark and vibrant color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getDarkVibrantSwatch()
     */
    @ColorInt
    public int getDarkVibrantColor(@ColorInt final int defaultColor) {
        return getColorForTarget(Target.DARK_VIBRANT, defaultColor);
    }

    /**
     * Returns a muted color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getMutedSwatch()
     */
    @ColorInt
    public int getMutedColor(@ColorInt final int defaultColor) {
        return getColorForTarget(Target.MUTED, defaultColor);
    }

    /**
     * Returns a muted and light color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getLightMutedSwatch()
     */
    @ColorInt
    public int getLightMutedColor(@ColorInt final int defaultColor) {
        return getColorForTarget(Target.LIGHT_MUTED, defaultColor);
    }

    /**
     * Returns a muted and dark color from the palette as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getDarkMutedSwatch()
     */
    @ColorInt
    public int getDarkMutedColor(@ColorInt final int defaultColor) {
        return getColorForTarget(Target.DARK_MUTED, defaultColor);
    }

    /**
     * Returns the selected swatch for the given target from the palette, or {@code null} if one
     * could not be found.
     */
    public @Nullable Swatch getSwatchForTarget(final @NotNull Target target) {
        return mSelectedSwatches.get(target);
    }

    /**
     * Returns the selected color for the given target from the palette as an RGB packed int.
     *
     * @param target       target that describes the properties of the color
     * @param defaultColor value to return if the swatch isn't available
     */
    @ColorInt
    public int getColorForTarget(final @NotNull Target target, @ColorInt final int defaultColor) {
        Swatch swatch = getSwatchForTarget(target);
        return swatch != null ? swatch.getRgb() : defaultColor;
    }

    /**
     * Returns the dominant swatch from the palette.
     *
     * <p>The dominant swatch is defined as the swatch with the greatest population (frequency)
     * within the palette.</p>
     */
    public @Nullable Swatch getDominantSwatch() {
        return mDominantSwatch;
    }

    /**
     * Returns the color of the dominant swatch from the palette, as an RGB packed int.
     *
     * @param defaultColor value to return if the swatch isn't available
     * @see #getDominantSwatch()
     */
    @ColorInt
    public int getDominantColor(@ColorInt int defaultColor) {
        return mDominantSwatch != null ? mDominantSwatch.getRgb() : defaultColor;
    }

    @SuppressWarnings("NullAway")
        // TODO(b/141959297): Suppressed during upgrade to AGP 3.6.
    void generate() {
        // We need to make sure that the scored targets are generated first. This is so that
        // inherited targets have something to inherit from
        for (int i = 0, count = mTargets.size(); i < count; i++) {
            final Target target = mTargets.get(i);
            target.normalizeWeights();
            mSelectedSwatches.put(target, generateScoredTarget(target));
        }
        // We now clear out the used colors
        mUsedColors.clear();
    }

    private @Nullable Swatch generateScoredTarget(final Target target) {
        final Swatch maxScoreSwatch = getMaxScoredSwatchForTarget(target);
        if (maxScoreSwatch != null && target.isExclusive()) {
            // If we have a swatch, and the target is exclusive, add the color to the used list
            mUsedColors.append(maxScoreSwatch.getRgb(), true);
        }
        return maxScoreSwatch;
    }

    private @Nullable Swatch getMaxScoredSwatchForTarget(final Target target) {
        float maxScore = 0;
        Swatch maxScoreSwatch = null;
        for (int i = 0, count = mSwatches.size(); i < count; i++) {
            final Swatch swatch = mSwatches.get(i);
            if (shouldBeScoredForTarget(swatch, target)) {
                final float score = generateScore(swatch, target);
                if (maxScoreSwatch == null || score > maxScore) {
                    maxScoreSwatch = swatch;
                    maxScore = score;
                }
            }
        }
        return maxScoreSwatch;
    }

    private boolean shouldBeScoredForTarget(final Swatch swatch, final Target target) {
        // Check whether the HSL values are within the correct ranges, and this color hasn't been used yet.
        final float hsl[] = swatch.getHsl();
        return hsl[1] >= target.getMinimumSaturation() && hsl[1] <= target.getMaximumSaturation()
                && hsl[2] >= target.getMinimumLightness() && hsl[2] <= target.getMaximumLightness()
                && !mUsedColors.get(swatch.getRgb());
    }

    private float generateScore(Swatch swatch, Target target) {
        final float[] hsl = swatch.getHsl();

        float saturationScore = 0;
        float luminanceScore = 0;
        float populationScore = 0;

        final int maxPopulation = mDominantSwatch != null ? mDominantSwatch.getPopulation() : 1;

        if (target.getSaturationWeight() > 0) {
            saturationScore = target.getSaturationWeight()
                    * (1f - Math.abs(hsl[1] - target.getTargetSaturation()));
        }
        if (target.getLightnessWeight() > 0) {
            luminanceScore = target.getLightnessWeight()
                    * (1f - Math.abs(hsl[2] - target.getTargetLightness()));
        }
        if (target.getPopulationWeight() > 0) {
            populationScore = target.getPopulationWeight()
                    * (swatch.getPopulation() / (float) maxPopulation);
        }

        return saturationScore + luminanceScore + populationScore;
    }

    private @Nullable Swatch findDominantSwatch() {
        int maxPop = Integer.MIN_VALUE;
        Swatch maxSwatch = null;
        for (int i = 0, count = mSwatches.size(); i < count; i++) {
            Swatch swatch = mSwatches.get(i);
            if (swatch.getPopulation() > maxPop) {
                maxSwatch = swatch;
                maxPop = swatch.getPopulation();
            }
        }
        return maxSwatch;
    }

    /**
     * Represents a color swatch generated from an image's palette. The RGB color can be retrieved
     * by calling {@link #getRgb()}.
     */
    public static final class Swatch {
        private final int mRed, mGreen, mBlue;
        private final int mRgb;
        private final int mPopulation;

        private boolean mGeneratedTextColors;
        private int mTitleTextColor;
        private int mBodyTextColor;

        private float @Nullable [] mHsl;

        public Swatch(@ColorInt int color, int population) {
            mRed = ColorUtils.red(color);
            mGreen = ColorUtils.green(color);
            mBlue = ColorUtils.blue(color);
            mRgb = color;
            mPopulation = population;
        }

        /**
         * @return this swatch's RGB color value
         */
        @ColorInt
        public int getRgb() {
            return mRgb;
        }

        /**
         * Return this swatch's HSL values.
         * hsv[0] is Hue [0 .. 360)
         * hsv[1] is Saturation [0...1]
         * hsv[2] is Lightness [0...1]
         */
        public float @NotNull [] getHsl() {
            if (mHsl == null) {
                mHsl = new float[3];
            }
            ColorUtils.RGBToHSL(mRed, mGreen, mBlue, mHsl);
            return mHsl;
        }

        /**
         * @return the number of pixels represented by this swatch
         */
        public int getPopulation() {
            return mPopulation;
        }

        /**
         * Returns an appropriate color to use for any 'title' text which is displayed over this
         * {@link Swatch}'s color. This color is guaranteed to have sufficient contrast.
         */
        @ColorInt
        public int getTitleTextColor() {
            ensureTextColorsGenerated();
            return mTitleTextColor;
        }

        /**
         * Returns an appropriate color to use for any 'body' text which is displayed over this
         * {@link Swatch}'s color. This color is guaranteed to have sufficient contrast.
         */
        @ColorInt
        public int getBodyTextColor() {
            ensureTextColorsGenerated();
            return mBodyTextColor;
        }

        private void ensureTextColorsGenerated() {
            if (!mGeneratedTextColors) {
                // First check white, as most colors will be dark
                final int lightBodyAlpha = ColorUtils.calculateMinimumAlpha(
                        ColorUtils.WHITE, mRgb, MIN_CONTRAST_BODY_TEXT);
                final int lightTitleAlpha = ColorUtils.calculateMinimumAlpha(
                        ColorUtils.WHITE, mRgb, MIN_CONTRAST_TITLE_TEXT);

                if (lightBodyAlpha != -1 && lightTitleAlpha != -1) {
                    // If we found valid light values, use them and return
                    mBodyTextColor = ColorUtils.setAlphaComponent(ColorUtils.WHITE, lightBodyAlpha);
                    mTitleTextColor = ColorUtils.setAlphaComponent(ColorUtils.WHITE, lightTitleAlpha);
                    mGeneratedTextColors = true;
                    return;
                }

                final int darkBodyAlpha = ColorUtils.calculateMinimumAlpha(
                        ColorUtils.BLACK, mRgb, MIN_CONTRAST_BODY_TEXT);
                final int darkTitleAlpha = ColorUtils.calculateMinimumAlpha(
                        ColorUtils.BLACK, mRgb, MIN_CONTRAST_TITLE_TEXT);

                if (darkBodyAlpha != -1 && darkTitleAlpha != -1) {
                    // If we found valid dark values, use them and return
                    mBodyTextColor = ColorUtils.setAlphaComponent(ColorUtils.BLACK, darkBodyAlpha);
                    mTitleTextColor = ColorUtils.setAlphaComponent(ColorUtils.BLACK, darkTitleAlpha);
                    mGeneratedTextColors = true;
                    return;
                }

                // If we reach here then we can not find title and body values which use the same
                // lightness, we need to use mismatched values
                mBodyTextColor = lightBodyAlpha != -1
                                 ? ColorUtils.setAlphaComponent(ColorUtils.WHITE, lightBodyAlpha)
                                 : ColorUtils.setAlphaComponent(ColorUtils.BLACK, darkBodyAlpha);
                mTitleTextColor = lightTitleAlpha != -1
                                  ? ColorUtils.setAlphaComponent(ColorUtils.WHITE, lightTitleAlpha)
                                  : ColorUtils.setAlphaComponent(ColorUtils.BLACK, darkTitleAlpha);
                mGeneratedTextColors = true;
            }
        }

        // TODO Remove once AGP 3.3. Fixed by I32b659c4e842ba5ac3d45b2d75b080b810fe1fe8.
        @Override
        public @NotNull String toString() {
            return getClass().getSimpleName()
                    + " [RGB: #" + Integer.toHexString(getRgb()) + ']'
                    + " [HSL: " + Arrays.toString(getHsl()) + ']'
                    + " [Population: " + mPopulation + ']'
                    + " [Title Text: #" + Integer.toHexString(getTitleTextColor())
                    + ']'
                    + " [Body Text: #" + Integer.toHexString(getBodyTextColor())
                    + ']';
        }

        @Override
        // TODO Remove @Nullable once AGP 3.3. Fixed by I32b659c4e842ba5ac3d45b2d75b080b810fe1fe8.
        public boolean equals(@Nullable Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Swatch swatch = (Swatch) o;
            return mPopulation == swatch.mPopulation && mRgb == swatch.mRgb;
        }

        @Override
        public int hashCode() {
            return 31 * mRgb + mPopulation;
        }
    }

    /**
     * Builder class for generating {@link Palette} instances.
     */
    public static final class Builder {
        private final @Nullable List<Swatch> mSwatches;
        private final @Nullable Bitmap mBitmap;

        private final List<Target> mTargets = new ArrayList<>();

        private int mMaxColors = DEFAULT_CALCULATE_NUMBER_COLORS;
        private int mResizeArea = DEFAULT_RESIZE_BITMAP_AREA;
        private int mResizeMaxDimension = -1;

        private final List<Filter> mFilters = new ArrayList<>();
        private @Nullable Rectangle mRegion;

        /**
         * Construct a new {@link Builder} using a source {@link Bitmap}
         */
        public Builder(@NotNull Bitmap bitmap) {
            if (bitmap == null || bitmap.isDisposed()) {
                throw new IllegalArgumentException("Bitmap is not valid");
            }
            mFilters.add(DEFAULT_FILTER);
            mBitmap = bitmap;
            mSwatches = null;

            // Add the default targets
            mTargets.add(Target.LIGHT_VIBRANT);
            mTargets.add(Target.VIBRANT);
            mTargets.add(Target.DARK_VIBRANT);
            mTargets.add(Target.LIGHT_MUTED);
            mTargets.add(Target.MUTED);
            mTargets.add(Target.DARK_MUTED);
        }

        /**
         * Construct a new {@link Builder} using a list of {@link Swatch} instances.
         * Typically only used for testing.
         */
        public Builder(@NotNull List<Swatch> swatches) {
            if (swatches == null || swatches.isEmpty()) {
                throw new IllegalArgumentException("List of Swatches is not valid");
            }
            mFilters.add(DEFAULT_FILTER);
            mSwatches = swatches;
            mBitmap = null;
        }

        /**
         * Set the maximum number of colors to use in the quantization step when using a
         * {@link android.graphics.Bitmap} as the source.
         * <p>
         * Good values for depend on the source image type. For landscapes, good values are in
         * the range 10-16. For images which are largely made up of people's faces then this
         * value should be increased to ~24.
         */
        public @NotNull Builder maximumColorCount(int colors) {
            mMaxColors = colors;
            return this;
        }

        /**
         * Set the resize value when using a {@link android.graphics.Bitmap} as the source.
         * If the bitmap's largest dimension is greater than the value specified, then the bitmap
         * will be resized so that its largest dimension matches {@code maxDimension}. If the
         * bitmap is smaller or equal, the original is used as-is.
         *
         * @param maxDimension the number of pixels that the max dimension should be scaled down to,
         *                     or any value <= 0 to disable resizing.
         * @deprecated Using {@link #resizeBitmapArea(int)} is preferred since it can handle
         * abnormal aspect ratios more gracefully.
         */
        @Deprecated
        public @NotNull Builder resizeBitmapSize(final int maxDimension) {
            mResizeMaxDimension = maxDimension;
            mResizeArea = -1;
            return this;
        }

        /**
         * Set the resize value when using a {@link android.graphics.Bitmap} as the source.
         * If the bitmap's area is greater than the value specified, then the bitmap
         * will be resized so that its area matches {@code area}. If the
         * bitmap is smaller or equal, the original is used as-is.
         * <p>
         * This value has a large effect on the processing time. The larger the resized image is,
         * the greater time it will take to generate the palette. The smaller the image is, the
         * more detail is lost in the resulting image and thus less precision for color selection.
         *
         * @param area the number of pixels that the intermediary scaled down Bitmap should cover,
         *             or any value <= 0 to disable resizing.
         */
        public @NotNull Builder resizeBitmapArea(final int area) {
            mResizeArea = area;
            mResizeMaxDimension = -1;
            return this;
        }

        /**
         * Clear all added filters. This includes any default filters added automatically by
         * {@link Palette}.
         */
        public @NotNull Builder clearFilters() {
            mFilters.clear();
            return this;
        }

        /**
         * Add a filter to be able to have fine grained control over which colors are
         * allowed in the resulting palette.
         *
         * @param filter filter to add.
         */
        public @NotNull Builder addFilter(@NotNull Filter filter) {
            if (filter != null) {
                mFilters.add(filter);
            }
            return this;
        }

        /**
         * Set a region of the bitmap to be used exclusively when calculating the palette.
         * <p>This only works when the original input is a {@link Bitmap}.</p>
         *
         * @param left   The left side of the rectangle used for the region.
         * @param top    The top of the rectangle used for the region.
         * @param right  The right side of the rectangle used for the region.
         * @param bottom The bottom of the rectangle used for the region.
         */
        public @NotNull Builder setRegion(int left, int top, int right, int bottom) {
            if (mBitmap != null) {
                if (mRegion == null) mRegion = new Rectangle();
                // Set the Rectangle to be initially the whole Bitmap
                mRegion.set(0, 0, mBitmap.getWidth(), mBitmap.getHeight());
                // Now just get the intersection with the region
//                if (!mRegion.intersect(left, top, right, bottom)) {
                if (!(mRegion.x < left + right && mRegion.x + mRegion.width > left && mRegion.y < top + bottom && mRegion.y + mRegion.height > top)) {
                    throw new IllegalArgumentException("The given region must intersect with "
                                                               + "the Bitmap's dimensions.");
                }
            }
            return this;
        }

        /**
         * Clear any previously region set via {@link #setRegion(int, int, int, int)}.
         */
        public @NotNull Builder clearRegion() {
            mRegion = null;
            return this;
        }

        /**
         * Add a target profile to be generated in the palette.
         *
         * <p>You can retrieve the result via {@link Palette#getSwatchForTarget(Target)}.</p>
         */
        public @NotNull Builder addTarget(final @NotNull Target target) {
            if (!mTargets.contains(target)) {
                mTargets.add(target);
            }
            return this;
        }

        /**
         * Clear all added targets. This includes any default targets added automatically by
         * {@link Palette}.
         */
        public @NotNull Builder clearTargets() {
            if (mTargets != null) {
                mTargets.clear();
            }
            return this;
        }

        /**
         * Generate and return the {@link Palette} synchronously.
         */
        public @NotNull Palette generate() {
            List<Swatch> swatches;

            if (mBitmap != null) {
                // We have a Bitmap so we need to use quantization to reduce the number of colors

                // First we'll scale down the bitmap if needed
                final Bitmap bitmap = scaleBitmapDown(mBitmap);

                final Rectangle region = mRegion;
                if (bitmap != mBitmap && region != null) {
                    // If we have a scaled bitmap and a selected region, we need to scale down the
                    // region to match the new scale
                    final double scale = bitmap.getWidth() / (double) mBitmap.getWidth();
                    region.x = (int) Math.floor(region.x * scale);
                    region.y = (int) Math.floor(region.y * scale);
                    region.width = Math.min((int) Math.ceil(region.width * scale),
                                            bitmap.getWidth());
                    region.height = Math.min((int) Math.ceil(region.height * scale),
                                             bitmap.getHeight());
                }

                // Now generate a quantizer from the Bitmap
                final ColorCutQuantizer quantizer = new ColorCutQuantizer(
                        getPixelsFromBitmap(bitmap),
                        mMaxColors,
                        mFilters.isEmpty() ? null : mFilters.toArray(new Filter[mFilters.size()]));

                // If created a new bitmap, recycle it
                if (bitmap != mBitmap) {
                    bitmap.dispose();
                }

                swatches = quantizer.getQuantizedColors();
            }
            else if (mSwatches != null) {
                // Else we're using the provided swatches
                swatches = mSwatches;
            }
            else {
                // The constructors enforce either a bitmap or swatches are present.
                throw new AssertionError();
            }

            // Now create a Palette instance
            final Palette p = new Palette(swatches, mTargets);
            // And make it generate itself
            p.generate();

            return p;
        }

        // todo: implement async load
//        /**
//         * Generate the {@link Palette} asynchronously. The provided listener's
//         * {@link PaletteAsyncListener#onGenerated} method will be called with the palette when
//         * generated.
//         *
//         * @deprecated Use the standard <code>java.util.concurrent</code> or
//         * <a href="https://developer.android.com/topic/libraries/architecture/coroutines">
//         * Kotlin concurrency utilities</a> to call {@link #generate()} instead.
//         */
//        @Deprecated
//        public android.os.@NotNull AsyncTask<Bitmap, Void, Palette> generate(final @NotNull PaletteAsyncListener listener) {
//            Preconditions.checkNotNull(listener);
//
//            return new android.os.AsyncTask<Bitmap, Void, Palette>() {
//                @Override
//                protected @Nullable Palette doInBackground(Bitmap... params) {
//                    try {
//                        return generate();
//                    }
//                    catch (Exception e) {
//                        Log.e(LOG_TAG, "Exception thrown during async generate", e);
//                        return null;
//                    }
//                }
//
//                @Override
//                protected void onPostExecute(@Nullable Palette colorExtractor) {
//                    listener.onGenerated(colorExtractor);
//                }
//            }.executeOnExecutor(android.os.AsyncTask.THREAD_POOL_EXECUTOR, mBitmap);
//        }

        private int[] getPixelsFromBitmap(Bitmap bitmap) {
            final int bitmapWidth = bitmap.getWidth();
            final int bitmapHeight = bitmap.getHeight();
            final int[] pixels = new int[bitmapWidth * bitmapHeight];
            bitmap.getPixels(pixels, 0, bitmapWidth, 0, 0, bitmapWidth, bitmapHeight);

            if (mRegion == null) {
                // If we don't have a region, return all of the pixels
                return pixels;
            }
            else {
                // If we do have a region, lets create a subset array containing only the region's
                // pixels
                final int regionWidth = (int) mRegion.width;
                final int regionHeight = (int) mRegion.height;
                // pixels contains all of the pixels, so we need to iterate through each row and
                // copy the regions pixels into a new smaller array
                final int[] subsetPixels = new int[regionWidth * regionHeight];
                for (int row = 0; row < regionHeight; row++) {
                    System.arraycopy(pixels, (int)((int) ((row + mRegion.y) * bitmapWidth) + mRegion.x),
                                     subsetPixels, row * regionWidth, regionWidth);
                }
                return subsetPixels;
            }
        }

        /**
         * Scale the bitmap down as needed.
         */
        private Bitmap scaleBitmapDown(final Bitmap bitmap) {
            double scaleRatio = -1;

            if (mResizeArea > 0) {
                final int bitmapArea = bitmap.getWidth() * bitmap.getHeight();
                if (bitmapArea > mResizeArea) {
                    scaleRatio = Math.sqrt(mResizeArea / (double) bitmapArea);
                }
            }
            else if (mResizeMaxDimension > 0) {
                final int maxDimension = Math.max(bitmap.getWidth(), bitmap.getHeight());
                if (maxDimension > mResizeMaxDimension) {
                    scaleRatio = mResizeMaxDimension / (double) maxDimension;
                }
            }

            if (scaleRatio <= 0) {
                // Scaling has been disabled or not needed so just return the Bitmap
                return bitmap;
            }

            return Bitmap.createScaledBitmap(bitmap,
                                             (int) Math.ceil(bitmap.getWidth() * scaleRatio),
                                             (int) Math.ceil(bitmap.getHeight() * scaleRatio),
                                             false);
        }
    }

    /**
     * A Filter provides a mechanism for exercising fine-grained control over which colors
     * are valid within a resulting {@link Palette}.
     */
    public interface Filter {
        /**
         * Hook to allow clients to be able filter colors from resulting palette.
         *
         * @param rgb the color in RGB888.
         * @param hsl HSL representation of the color.
         * @return true if the color is allowed, false if not.
         * @see Builder#addFilter(Filter)
         */
        boolean isAllowed(@ColorInt int rgb, float @NotNull [] hsl);
    }

    /**
     * The default filter.
     */
    static final Filter DEFAULT_FILTER = new Filter() {
        private static final float BLACK_MAX_LIGHTNESS = 0.05f;
        private static final float WHITE_MIN_LIGHTNESS = 0.95f;

        @Override
        public boolean isAllowed(int rgb, float[] hsl) {
            return !isWhite(hsl) && !isBlack(hsl) && !isNearRedILine(hsl);
        }

        /**
         * @return true if the color represents a color which is close to black.
         */
        private boolean isBlack(float[] hslColor) {
            return hslColor[2] <= BLACK_MAX_LIGHTNESS;
        }

        /**
         * @return true if the color represents a color which is close to white.
         */
        private boolean isWhite(float[] hslColor) {
            return hslColor[2] >= WHITE_MIN_LIGHTNESS;
        }

        /**
         * @return true if the color lies close to the red side of the I line.
         */
        private boolean isNearRedILine(float[] hslColor) {
            return hslColor[0] >= 10f && hslColor[0] <= 37f && hslColor[1] <= 0.82f;
        }
    };
}
