/**
 * Copyright 2004 - 2016 Syncleus, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.syncleus.ferma.framefactories.annotation;

import net.bytebuddy.dynamic.DynamicType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Allows handling of method on frames. Only the first method handler found is called.
 * Instances of this class should be threadsafe.
 */
public interface MethodHandler {

    /**
     * @return The annotation type that this handler responds to.
     */
    Class<? extends Annotation> getAnnotationType();

    /**
     * @param <E> The loaded type of the Byte Buddy Builder
     * @param method The method being called on the frame.
     * @param annotation The annotation
     * @param builder ByteBuddy Builder class to expand.
     * @return A return value for the method.
     */
    <E> DynamicType.Builder<E> processMethod(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation);
}
