/*
 * Copyright (C) 2015 The Android Open Source Project
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
package androidx.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated element should be a float or double in the given range
 *
 * Example:
 * ```
 *
 * @FloatRange(from=0.0,to=1.0) public float getAlpha() {
 * ...
 * }
 * ```
 */


@Documented
@Retention(RetentionPolicy.CLASS)
//@Target(
//        AnnotationTarget.FUNCTION,
//        AnnotationTarget.PROPERTY_GETTER,
//        AnnotationTarget.PROPERTY_SETTER,
//        AnnotationTarget.VALUE_PARAMETER,
//        AnnotationTarget.FIELD,
//        AnnotationTarget.LOCAL_VARIABLE,
//        AnnotationTarget.ANNOTATION_CLASS,
//)
@Target({
        ElementType.METHOD,
        ElementType.PARAMETER,
        ElementType.FIELD,
        ElementType.LOCAL_VARIABLE,
        ElementType.ANNOTATION_TYPE
})
public @interface FloatRange {
    /** Smallest value. Whether it is inclusive or not is determined by [.fromInclusive] */
    double from() default Double.NEGATIVE_INFINITY;

    /** Largest value. Whether it is inclusive or not is determined by [.toInclusive] */
    double to() default Double.POSITIVE_INFINITY;

    /** Whether the from value is included in the range */
    boolean fromInclusive() default true;

    /** Whether the to value is included in the range */
    boolean toInclusive() default true;
}