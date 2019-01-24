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

import com.syncleus.ferma.EdgeFrame;
import com.syncleus.ferma.annotations.InVertex;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * A method handler that implemented the InVertex Annotation.
 *
 * @since 2.0.0
 */
public class InVertexMethodHandler implements MethodHandler {

    @Override
    public Class<InVertex> getAnnotationType() {
        return InVertex.class;
    }

    @Override
    public <E> DynamicType.Builder<E> processMethod(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        final java.lang.reflect.Parameter[] arguments = method.getParameters();

        if (ReflectionUtility.isGetMethod(method))
            if (arguments == null || arguments.length == 0)
                return this.getNode(builder, method, annotation);
            else
                throw new IllegalStateException(method.getName() + " was annotated with @InVertex but had arguments.");
        else
            throw new IllegalStateException(method.getName() + " was annotated with @InVertex but did not begin with: get");
    }

    private <E> DynamicType.Builder<E> getNode(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(getVertexInterceptor.class));
    }

    public static final class getVertexInterceptor {

        @RuntimeType
        public static Object getVertex(@This final EdgeFrame thiz, @Origin final Method method) {
            return thiz.inV().next(method.getReturnType());
        }
    }
}
