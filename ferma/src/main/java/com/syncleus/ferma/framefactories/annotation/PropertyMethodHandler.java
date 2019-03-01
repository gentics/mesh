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

import com.syncleus.ferma.ElementFrame;
import com.syncleus.ferma.annotations.Property;
import net.bytebuddy.dynamic.DynamicType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * A method handler that implemented the Property Annotation.
 *
 * @since 2.0.0
 */
public class PropertyMethodHandler implements MethodHandler {

    @Override
    public Class<Property> getAnnotationType() {
        return Property.class;
    }

    @Override
    public <E> DynamicType.Builder<E> processMethod(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        final java.lang.reflect.Parameter[] arguments = method.getParameters();

        if (ReflectionUtility.isSetMethod(method))
            if (arguments == null || arguments.length == 0)
                throw new IllegalStateException(method.getName() + " was annotated with @Property but had no arguments.");
            else if (arguments.length == 1)
                return this.setProperty(builder, method, annotation);
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Property but had more than 1 arguments.");
        else if (ReflectionUtility.isGetMethod(method))
            if (arguments == null || arguments.length == 0)
                return this.getProperty(builder, method, annotation);
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Property but had arguments.");
        else if (ReflectionUtility.isRemoveMethod(method))
            if (arguments == null || arguments.length == 0)
                return this.removeProperty(builder, method, annotation);
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Property but had some arguments.");
        else
            throw new IllegalStateException(method.getName() + " was annotated with @Property but did not begin with either of the following keywords: add, get");
    }

    private <E> DynamicType.Builder<E> setProperty(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(SetPropertyInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getProperty(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetPropertyInterceptor.class));
    }

    private <E> DynamicType.Builder<E> removeProperty(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(RemovePropertyInterceptor.class));
    }

    private static Enum getValueAsEnum(final Method method, final Object value) {
        final Class<Enum> en = (Class<Enum>) method.getReturnType();
        if (value != null)
            return Enum.valueOf(en, value.toString());

        return null;
    }

    public static final class GetPropertyInterceptor {

        @RuntimeType
        public static Object getProperty(@This final ElementFrame thiz, @Origin final Method method) {
            assert thiz instanceof CachesReflection;
            final Property annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Property.class);
            final String value = annotation.value();

            final Object obj = thiz.getProperty(value);
            if (method.getReturnType().isEnum())
                return getValueAsEnum(method, obj);
            else
                return obj;
        }
    }

    public static final class SetPropertyInterceptor {

        @RuntimeType
        public static void setProperty(@This final ElementFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final Object obj) {
            assert thiz instanceof CachesReflection;
            final Property annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Property.class);
            final String value = annotation.value();

            if (obj != null && obj.getClass().isEnum())
                thiz.setProperty(value, ((Enum<?>) obj).name());
            else
                thiz.setProperty(value, obj);
        }
    }

    public static final class RemovePropertyInterceptor {

        public static void removeProperty(@This final ElementFrame thiz, @Origin final Method method) {
            assert thiz instanceof CachesReflection;
            final Property annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Property.class);
            final String value = annotation.value();

            thiz.getElement().removeProperty(value);
        }
    }
}
