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

import com.syncleus.ferma.typeresolvers.TypeResolver;
import com.syncleus.ferma.*;
import com.syncleus.ferma.annotations.Incidence;
import com.tinkerpop.blueprints.Direction;

import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import net.bytebuddy.matcher.ElementMatchers;

/**
 * A TinkerPop method handler that implemented the Incidence Annotation.
 *
 * @since 2.0.0
 */
public class IncidenceMethodHandler implements MethodHandler {

    @Override
    public Class<Incidence> getAnnotationType() {
        return Incidence.class;
    }

    @Override
    public <E> DynamicType.Builder<E> processMethod(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        final java.lang.reflect.Parameter[] arguments = method.getParameters();

        if (ReflectionUtility.isAddMethod(method))
            if (arguments == null || arguments.length == 0)
                return this.addEdgeDefault(builder, method, annotation);
            else if (arguments.length == 1)
                if (ClassInitializer.class.isAssignableFrom(arguments[0].getType()))
                    return this.addEdgeByTypeUntypedEdge(builder, method, annotation);
                else
                    return this.addEdgeByObjectUntypedEdge(builder, method, annotation);
            else if (arguments.length == 2) {
                if (!(ClassInitializer.class.isAssignableFrom(arguments[1].getType())))
                    throw new IllegalStateException(method.getName() + " was annotated with @Adjacency, had two arguments, but the second argument was not of the type ClassInitializer");

                if (ClassInitializer.class.isAssignableFrom(arguments[0].getType()))
                    return this.addEdgeByTypeTypedEdge(builder, method, annotation);
                else
                    return this.addEdgeByObjectTypedEdge(builder, method, annotation);
            }
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had more than 1 arguments.");
        if (ReflectionUtility.isGetMethod(method))
            if (arguments == null || arguments.length == 0)
                if (Iterable.class.isAssignableFrom(method.getReturnType()))
                    return this.getEdgesDefault(builder, method, annotation);
                else
                    return this.getEdgeDefault(builder, method, annotation);
            else if (arguments.length == 1) {
                if (!(Class.class.isAssignableFrom(arguments[0].getType())))
                    throw new IllegalStateException(method.getName() + " was annotated with @Incidence, had a single argument, but that argument was not of the type Class");

                if (Iterable.class.isAssignableFrom(method.getReturnType()))
                    return this.getEdgesByType(builder, method, annotation);

                return this.getEdgeByType(builder, method, annotation);
            }
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Incidence but had more than 1 arguments.");
        else if (ReflectionUtility.isRemoveMethod(method))
            if (arguments == null || arguments.length == 0)
                throw new IllegalStateException(method.getName() + " was annotated with @Incidence but had no arguments.");
            else if (arguments.length == 1)
                return this.removeEdge(builder, method, annotation);
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Incidence but had more than 1 arguments.");
        else
            throw new IllegalStateException(method.getName() + " was annotated with @Incidence but did not begin with: get, remove");
    }

    private <E> DynamicType.Builder<E> addEdgeDefault(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddEdgeDefaultInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addEdgeByTypeUntypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddEdgeByTypeUntypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addEdgeByObjectUntypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddEdgeByObjectUntypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addEdgeByTypeTypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddEdgeByTypeTypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addEdgeByObjectTypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddEdgeByObjectTypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getEdgesDefault(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetEdgesDefaultInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getEdgesByType(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetEdgesByTypeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getEdgeDefault(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetEdgeDefaultInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getEdgeByType(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetEdgeByTypeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> removeEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(RemoveEdgeInterceptor.class));
    }

    public static final class AddEdgeDefaultInterceptor {

        @RuntimeType
        public static Object addEdge(@This final VertexFrame thiz, @Origin final Method method) {
            final VertexFrame newVertex = thiz.getGraph().addFramedVertex();
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    throw new IllegalStateException(method.getName() + " is annotated with direction BOTH, this is not allowed for add methods annotated with @Incidence.");
                case IN:
                    return thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                case OUT:
                    return thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class AddEdgeByTypeUntypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(value = 0) final ClassInitializer vertexType) {
            final Object newNode = thiz.getGraph().addFramedVertex(null, vertexType);
            assert newNode instanceof VertexFrame;
            final VertexFrame newVertex = ((VertexFrame) newNode);

            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            assert vertexType.getInitializationType().isInstance(newNode);

            switch (direction) {
                case BOTH:
                    throw new IllegalStateException(method.getName() + " is annotated with direction BOTH, this is not allowed for add methods annotated with @Incidence.");
                case IN:
                    return thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                case OUT:
                    return thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class AddEdgeByTypeTypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final ClassInitializer vertexType, @RuntimeType @Argument(1) final ClassInitializer edgeType) {
            final Object newNode = thiz.getGraph().addFramedVertex(null, vertexType);
            assert newNode instanceof VertexFrame;
            final VertexFrame newVertex = ((VertexFrame) newNode);

            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            assert vertexType.getInitializationType().isInstance(newNode);

            switch (direction) {
                case BOTH:
                    throw new IllegalStateException(method.getName() + " is annotated with direction BOTH, this is not allowed for add methods annotated with @Incidence.");
                case IN:
                    return thiz.getGraph().addFramedEdge(null, newVertex, thiz, label, edgeType);
                case OUT:
                    return thiz.getGraph().addFramedEdge(null, thiz, newVertex, label, edgeType);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class AddEdgeByObjectUntypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final VertexFrame newVertex) {
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    throw new IllegalStateException(method.getName() + " is annotated with direction BOTH, this is not allowed for add methods annotated with @Incidence.");
                case IN:
                    return thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                case OUT:
                    return thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class AddEdgeByObjectTypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final VertexFrame newVertex, @RuntimeType @Argument(1) final ClassInitializer edgeType) {
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    throw new IllegalStateException(method.getName() + " is annotated with direction BOTH, this is not allowed for add methods annotated with @Incidence.");
                case IN:
                    return thiz.getGraph().addFramedEdge(null, newVertex, thiz, label, edgeType);
                case OUT:
                    return thiz.getGraph().addFramedEdge(null, thiz, newVertex, label, edgeType);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class GetEdgesDefaultInterceptor {

        @RuntimeType
        public static Iterable getEdges(@This final VertexFrame thiz, @Origin final Method method) {
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    return thiz.bothE(label).frame(VertexFrame.class);
                case IN:
                    return thiz.inE(label).frame(VertexFrame.class);
                case OUT:
                    return thiz.outE(label).frame(VertexFrame.class);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class GetEdgesByTypeInterceptor {

        @RuntimeType
        public static Iterable getEdges(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final Class type) {
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();
            final TypeResolver resolver = thiz.getGraph().getTypeResolver();

            switch (direction) {
                case BOTH:
                    return resolver.hasType(thiz.bothE(label), type).frame(type);
                case IN:
                    return resolver.hasType(thiz.inE(label), type).frame(type);
                case OUT:
                    return resolver.hasType(thiz.outE(label), type).frame(type);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class GetEdgeDefaultInterceptor {

        @RuntimeType
        public static Object getEdges(@This final VertexFrame thiz, @Origin final Method method) {
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    return thiz.bothE(label).next(VertexFrame.class);
                case IN:
                    return thiz.inE(label).next(VertexFrame.class);
                case OUT:
                    return thiz.outE(label).next(VertexFrame.class);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class GetEdgeByTypeInterceptor {

        @RuntimeType
        public static Object getEdge(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final Class type) {
            assert thiz instanceof CachesReflection;
            final Incidence annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Incidence.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();
            final TypeResolver resolver = thiz.getGraph().getTypeResolver();

            switch (direction) {
                case BOTH:
                    return resolver.hasType(thiz.bothE(label), type).next(type);
                case IN:
                    return resolver.hasType(thiz.inE(label), type).next(type);
                case OUT:
                    return resolver.hasType(thiz.outE(label), type).next(type);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class RemoveEdgeInterceptor {

        @RuntimeType
        public static void removeEdge(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final EdgeFrame edge) {
            edge.remove();
        }
    }
}
