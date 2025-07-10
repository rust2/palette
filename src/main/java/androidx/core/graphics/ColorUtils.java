package androidx.core.graphics;

import androidx.annotation.ColorInt;
import androidx.annotation.FloatRange;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

public class ColorUtils {
    private static final int MIN_ALPHA_SEARCH_MAX_ITERATIONS = 10;
    private static final int MIN_ALPHA_SEARCH_PRECISION = 1;

    private static final ThreadLocal<double[]> TEMP_ARRAY = new ThreadLocal<>();

    private ColorUtils() {}

    /**
     * Composite two potentially translucent colors over each other and returns the result.
     */
    public static int compositeColors(@ColorInt int foreground, @ColorInt int background) {
        int bgAlpha = alpha(background);
        int fgAlpha = alpha(foreground);
        int a = compositeAlpha(fgAlpha, bgAlpha);

        int r = compositeComponent(red(foreground), fgAlpha,
                                   red(background), bgAlpha, a);
        int g = compositeComponent(green(foreground), fgAlpha,
                                   green(background), bgAlpha, a);
        int b = compositeComponent(blue(foreground), fgAlpha,
                                   blue(background), bgAlpha, a);

        return argb(a, r, g, b);
    }

    @SuppressWarnings("SameParameterValue")
    private static float constrain(float amount, float low, float high) {
        return amount < low ? low : Math.min(amount, high);
    }

    /**
     * Convert the ARGB color to its HSL (hue-saturation-lightness) components.
     * <ul>
     * <li>outHsl[0] is Hue [0, 360)</li>
     * <li>outHsl[1] is Saturation [0, 1]</li>
     * <li>outHsl[2] is Lightness [0, 1]</li>
     * </ul>
     *
     * @param color  the ARGB color to convert. The alpha component is ignored
     * @param outHsl 3-element array which holds the resulting HSL components
     */
    public static void colorToHSL(@ColorInt int color, float @NotNull [] outHsl) {
        RGBToHSL(red(color), green(color), blue(color), outHsl);
    }

    /**
     * Convert RGB components to HSL (hue-saturation-lightness).
     * <ul>
     * <li>outHsl[0] is Hue [0, 360)</li>
     * <li>outHsl[1] is Saturation [0, 1]</li>
     * <li>outHsl[2] is Lightness [0, 1]</li>
     * </ul>
     *
     * @param r      red component value [0, 255]
     * @param g      green component value [0, 255]
     * @param b      blue component value [0, 255]
     * @param outHsl 3-element array which holds the resulting HSL components
     */
    public static void RGBToHSL(@Range(from = 0x0, to = 0xFF) int r,
                                @Range(from = 0x0, to = 0xFF) int g, @Range(from = 0x0, to = 0xFF) int b,
                                float @NotNull [] outHsl) {
        final float rf = r / 255f;
        final float gf = g / 255f;
        final float bf = b / 255f;

        final float max = Math.max(rf, Math.max(gf, bf));
        final float min = Math.min(rf, Math.min(gf, bf));
        final float deltaMaxMin = max - min;

        float h, s;
        float l = (max + min) / 2f;

        if (max == min) {
            // Monochromatic
            h = s = 0f;
        }
        else {
            if (max == rf) {
                h = ((gf - bf) / deltaMaxMin) % 6f;
            }
            else if (max == gf) {
                h = ((bf - rf) / deltaMaxMin) + 2f;
            }
            else {
                h = ((rf - gf) / deltaMaxMin) + 4f;
            }

            s = deltaMaxMin / (1f - Math.abs(2f * l - 1f));
        }

        h = (h * 60f) % 360f;
        if (h < 0) {
            h += 360f;
        }

        outHsl[0] = constrain(h, 0f, 360f);
        outHsl[1] = constrain(s, 0f, 1f);
        outHsl[2] = constrain(l, 0f, 1f);
    }

    /**
     * Set the alpha component of {@code color} to be {@code alpha}.
     */
    @ColorInt
    public static int setAlphaComponent(@ColorInt int color,
                                        @Range(from = 0x0, to = 0xFF) int alpha) {
        if (alpha < 0 || alpha > 255) {
            throw new IllegalArgumentException("alpha must be between 0 and 255.");
        }
        return (color & 0x00ffffff) | (alpha << 24);
    }

    /**
     * Calculates the minimum alpha value which can be applied to {@code foreground} so that would
     * have a contrast value of at least {@code minContrastRatio} when compared to
     * {@code background}.
     *
     * @param foreground       the foreground color
     * @param background       the opaque background color
     * @param minContrastRatio the minimum contrast ratio
     * @return the alpha value in the range [0, 255] or -1 if no value could be calculated
     */
    public static int calculateMinimumAlpha(@ColorInt int foreground, @ColorInt int background,
                                            float minContrastRatio) {
        if (alpha(background) != 255) {
            throw new IllegalArgumentException("background can not be translucent: #"
                                                       + Integer.toHexString(background));
        }

        // First lets check that a fully opaque foreground has sufficient contrast
        int testForeground = setAlphaComponent(foreground, 255);
        double testRatio = calculateContrast(testForeground, background);
        if (testRatio < minContrastRatio) {
            // Fully opaque foreground does not have sufficient contrast, return error
            return -1;
        }

        // Binary search to find a value with the minimum value which provides sufficient contrast
        int numIterations = 0;
        int minAlpha = 0;
        int maxAlpha = 255;

        while (numIterations <= MIN_ALPHA_SEARCH_MAX_ITERATIONS &&
                (maxAlpha - minAlpha) > MIN_ALPHA_SEARCH_PRECISION) {
            final int testAlpha = (minAlpha + maxAlpha) / 2;

            testForeground = setAlphaComponent(foreground, testAlpha);
            testRatio = calculateContrast(testForeground, background);

            if (testRatio < minContrastRatio) {
                minAlpha = testAlpha;
            }
            else {
                maxAlpha = testAlpha;
            }

            numIterations++;
        }

        // Conservatively return the max of the range of possible alphas, which is known to pass.
        return maxAlpha;
    }

    /**
     * Returns the contrast ratio between {@code foreground} and {@code background}.
     * {@code background} must be opaque.
     * <p>
     * Formula defined
     * <a href="http://www.w3.org/TR/2008/REC-WCAG20-20081211/#contrast-ratiodef">here</a>.
     */
    public static double calculateContrast(@ColorInt int foreground, @ColorInt int background) {
        if (alpha(background) != 255) {
            throw new IllegalArgumentException("background can not be translucent: #"
                                                       + Integer.toHexString(background));
        }
        if (alpha(foreground) < 255) {
            // If the foreground is translucent, composite the foreground over the background
            foreground = compositeColors(foreground, background);
        }

        final double luminance1 = calculateLuminance(foreground) + 0.05;
        final double luminance2 = calculateLuminance(background) + 0.05;

        // Now return the lighter luminance divided by the darker luminance
        return Math.max(luminance1, luminance2) / Math.min(luminance1, luminance2);
    }

    private static int compositeAlpha(int foregroundAlpha, int backgroundAlpha) {
        return 0xFF - (((0xFF - backgroundAlpha) * (0xFF - foregroundAlpha)) / 0xFF);
    }

    private static int compositeComponent(int fgC, int fgA, int bgC, int bgA, int a) {
        if (a == 0) return 0;
        return ((0xFF * fgC * fgA) + (bgC * bgA * (0xFF - fgA))) / (a * 0xFF);
    }

    /**
     * Returns the luminance of a color as a float between {@code 0.0} and {@code 1.0}.
     * <p>Defined as the Y component in the XYZ representation of {@code color}.</p>
     */
    @FloatRange(from = 0.0, to = 1.0)
    public static double calculateLuminance(@ColorInt int color) {
        final double[] result = getTempDouble3Array();
        colorToXYZ(color, result);
        // Luminance is the Y component
        return result[1] / 100;
    }

    /**
     * Convert the ARGB color to its CIE XYZ representative components.
     *
     * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).</p>
     *
     * <ul>
     * <li>outXyz[0] is X [0, 95.047)</li>
     * <li>outXyz[1] is Y [0, 100)</li>
     * <li>outXyz[2] is Z [0, 108.883)</li>
     * </ul>
     *
     * @param color  the ARGB color to convert. The alpha component is ignored
     * @param outXyz 3-element array which holds the resulting LAB components
     */
    public static void colorToXYZ(@ColorInt int color, double @NotNull [] outXyz) {
        RGBToXYZ(red(color), green(color), blue(color), outXyz);
    }

    /**
     * Convert RGB components to its CIE XYZ representative components.
     *
     * <p>The resulting XYZ representation will use the D65 illuminant and the CIE
     * 2° Standard Observer (1931).</p>
     *
     * <ul>
     * <li>outXyz[0] is X [0, 95.047)</li>
     * <li>outXyz[1] is Y [0, 100)</li>
     * <li>outXyz[2] is Z [0, 108.883)</li>
     * </ul>
     *
     * @param r      red component value [0, 255]
     * @param g      green component value [0, 255]
     * @param b      blue component value [0, 255]
     * @param outXyz 3-element array which holds the resulting XYZ components
     */
    public static void RGBToXYZ(@Range(from = 0x0, to = 0xFF) int r,
                                @Range(from = 0x0, to = 0xFF) int g, @Range(from = 0x0, to = 0xFF) int b,
                                double @NotNull [] outXyz) {
        if (outXyz.length != 3) {
            throw new IllegalArgumentException("outXyz must have a length of 3.");
        }

        double sr = r / 255.0;
        sr = sr < 0.04045 ? sr / 12.92 : Math.pow((sr + 0.055) / 1.055, 2.4);
        double sg = g / 255.0;
        sg = sg < 0.04045 ? sg / 12.92 : Math.pow((sg + 0.055) / 1.055, 2.4);
        double sb = b / 255.0;
        sb = sb < 0.04045 ? sb / 12.92 : Math.pow((sb + 0.055) / 1.055, 2.4);

        outXyz[0] = 100 * (sr * 0.4124 + sg * 0.3576 + sb * 0.1805);
        outXyz[1] = 100 * (sr * 0.2126 + sg * 0.7152 + sb * 0.0722);
        outXyz[2] = 100 * (sr * 0.0193 + sg * 0.1192 + sb * 0.9505);
    }

    private static double[] getTempDouble3Array() {
        double[] result = TEMP_ARRAY.get();
        if (result == null) {
            result = new double[3];
            TEMP_ARRAY.set(result);
        }
        return result;
    }

    /// Merged from android.graphics.Color

    public static final int WHITE = 0xFFFFFFFF;
    public static final int BLACK = 0x00000000;

    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    public static int alpha(int color) {
        return color >>> 24;
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    public static int red(int color) {
        return (color >> 16) & 0xFF;
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    public static int green(int color) {
        return (color >> 8) & 0xFF;
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    public static int blue(int color) {
        return color & 0xFF;
    }

    /**
     * Return a color-int from red, green, blue components.
     * The alpha component is implicity 255 (fully opaque).
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     *
     * @param red   Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    @ColorInt
    public static int rgb(int red, int green, int blue) {
        return (0xFF << 24) | (red << 16) | (green << 8) | blue;
    }

    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     *
     * @param alpha Alpha component [0..255] of the color
     * @param red   Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    @ColorInt
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    public static int rgbaToArgb(int rgbaPixel) {
        // Shift red and green components right, shift alpha left
        // (rgba -> argb byte rotation)
        return (rgbaPixel >>> 8) | (rgbaPixel << 24);
    }

    public static int argbToRgba(int argbPixel) {
        // Shift red and green components left, shift alpha right
        return (argbPixel << 8) | ((argbPixel >> 24) & 0xFF);
    }
}
