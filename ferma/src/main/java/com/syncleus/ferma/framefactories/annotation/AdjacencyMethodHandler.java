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
import com.syncleus.ferma.annotations.Adjacency;
import com.tinkerpop.blueprints.Direction;
import net.bytebuddy.dynamic.DynamicType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.implementation.bind.annotation.Argument;
import net.bytebuddy.implementation.bind.annotation.Origin;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.This;

/**
 * A method handler that implemented the Adjacency Annotation.
 *
 * @since 2.0.0
 */
public class AdjacencyMethodHandler implements MethodHandler {

    @Override
    public Class<Adjacency> getAnnotationType() {
        return Adjacency.class;
    }

    @Override
    public <E> DynamicType.Builder<E> processMethod(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        final java.lang.reflect.Parameter[] arguments = method.getParameters();

        if (ReflectionUtility.isAddMethod(method))
            if (arguments == null || arguments.length == 0)
                return this.addVertexDefault(builder, method, annotation);
            else if (arguments.length == 1)
                if (ClassInitializer.class.isAssignableFrom(arguments[0].getType()))
                    return this.addVertexByTypeUntypedEdge(builder, method, annotation);
                else
                    return this.addVertexByObjectUntypedEdge(builder, method, annotation);
            else if (arguments.length == 2) {
                if (!(ClassInitializer.class.isAssignableFrom(arguments[1].getType())))
                    throw new IllegalStateException(method.getName() + " was annotated with @Adjacency, had two arguments, but the second argument was not of the type ClassInitializer");

                if (ClassInitializer.class.isAssignableFrom(arguments[0].getType()))
                    return this.addVertexByTypeTypedEdge(builder, method, annotation);
                else
                    return this.addVertexByObjectTypedEdge(builder, method, annotation);
            }
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had more than 1 arguments.");
        else if (ReflectionUtility.isGetMethod(method))
            if (arguments == null || arguments.length == 0) {
                if (Iterable.class.isAssignableFrom(method.getReturnType()))
                    return this.getVertexesDefault(builder, method, annotation);

                return this.getVertexDefault(builder, method, annotation);
            }
            else if (arguments.length == 1) {
                if (!(Class.class.isAssignableFrom(arguments[0].getType())))
                    throw new IllegalStateException(method.getName() + " was annotated with @Adjacency, had a single argument, but that argument was not of the type Class");

                if (Iterable.class.isAssignableFrom(method.getReturnType()))
                    return this.getVertexesByType(builder, method, annotation);

                return this.getVertexByType(builder, method, annotation);
            }
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had more than 1 arguments.");
        else if (ReflectionUtility.isRemoveMethod(method))
            if (arguments == null || arguments.length == 0)
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had no arguments.");
            else if (arguments.length == 1)
                return this.removeVertex(builder, method, annotation);
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had more than 1 arguments.");
        else if (ReflectionUtility.isSetMethod(method))
            if (arguments == null || arguments.length == 0)
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had no arguments.");
            else if (arguments.length == 1) {
                if (!(Iterable.class.isAssignableFrom(arguments[0].getType())))
                    throw new IllegalStateException(method.getName() + " was annotated with @Adjacency, had a single argument, but that argument was not of the type Class");

                return this.setVertex(builder, method, annotation);
            }
            else
                throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but had more than 1 arguments.");
        else
            throw new IllegalStateException(method.getName() + " was annotated with @Adjacency but did not begin with either of the following keywords: add, get, remove");
    }

    private <E> DynamicType.Builder<E> getVertexesDefault(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetVertexesDefaultInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getVertexDefault(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetVertexDefaultInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getVertexesByType(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetVertexesByTypeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> getVertexByType(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(GetVertexByTypeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addVertexDefault(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddVertexDefaultInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addVertexByTypeUntypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddVertexByTypeUntypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addVertexByObjectUntypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddVertexByObjectUntypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addVertexByTypeTypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddVertexByTypeTypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> addVertexByObjectTypedEdge(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(AddVertexByObjectTypedEdgeInterceptor.class));
    }

    private <E> DynamicType.Builder<E> setVertex(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(SetVertexInterceptor.class));
    }

    private <E> DynamicType.Builder<E> removeVertex(final DynamicType.Builder<E> builder, final Method method, final Annotation annotation) {
        return builder.method(ElementMatchers.is(method)).intercept(MethodDelegation.to(RemoveVertexInterceptor.class));
    }

    public static final class GetVertexesDefaultInterceptor {

        @RuntimeType
        public static Iterable getVertexes(@This final VertexFrame thiz, @Origin final Method method) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            return new FramingVertexIterable(thiz.getGraph(), thiz.getElement().getVertices(direction, label), VertexFrame.class);
        }
    }

    public static final class GetVertexesByTypeInterceptor {

        @RuntimeType
        public static Iterable getVertexes(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final Class type) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();
            final TypeResolver resolver = thiz.getGraph().getTypeResolver();

            switch (direction) {
                case BOTH:
                    return resolver.hasType(thiz.both(label), type).frame(type);
                case IN:
                    return resolver.hasType(thiz.in(label), type).frame(type);
                case OUT:
                    return resolver.hasType(thiz.out(label), type).frame(type);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class GetVertexDefaultInterceptor {

        @RuntimeType
        public static Object getVertexes(@This final VertexFrame thiz, @Origin final Method method) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    return thiz.both(label).next(VertexFrame.class);
                case IN:
                    return thiz.in(label).next(VertexFrame.class);
                case OUT:
                    return thiz.out(label).next(VertexFrame.class);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class GetVertexByTypeInterceptor {

        @RuntimeType
        public static Object getVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final Class type) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();
            final TypeResolver resolver = thiz.getGraph().getTypeResolver();
            
            switch (direction) {
                case BOTH:
                    return resolver.hasType(thiz.both(label), type).next(type);
                case IN:
                    return resolver.hasType(thiz.in(label), type).next(type);
                case OUT:
                    return resolver.hasType(thiz.out(label), type).next(type);
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class AddVertexDefaultInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method) {
            final VertexFrame newVertex = thiz.getGraph().addFramedVertex();
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                case IN:
                    thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    break;
                case OUT:
                    thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }

            return newVertex;
        }
    }

    public static final class AddVertexByTypeUntypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(value = 0) final ClassInitializer vertexType) {
            final Object newNode = thiz.getGraph().addFramedVertex(null, vertexType);
            assert newNode instanceof VertexFrame;
            final VertexFrame newVertex = ((VertexFrame) newNode);

            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            assert vertexType.getInitializationType().isInstance(newNode);

            switch (direction) {
                case BOTH:
                    thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                case IN:
                    thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    break;
                case OUT:
                    thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }

            return newNode;
        }
    }

    public static final class AddVertexByTypeTypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final ClassInitializer vertexType, @RuntimeType @Argument(1) final ClassInitializer edgeType) {
            final Object newNode = thiz.getGraph().addFramedVertex(null, vertexType);
            assert newNode instanceof VertexFrame;
            final VertexFrame newVertex = ((VertexFrame) newNode);

            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            assert vertexType.getInitializationType().isInstance(newNode);

            switch (direction) {
                case BOTH:
                    thiz.getGraph().addFramedEdge(null, newVertex, thiz, label, edgeType);
                    thiz.getGraph().addFramedEdge(null, thiz, newVertex, label, edgeType);
                    break;
                case IN:
                    thiz.getGraph().addFramedEdge(null, newVertex, thiz, label, edgeType);
                    break;
                case OUT:
                    thiz.getGraph().addFramedEdge(null, thiz, newVertex, label, edgeType);
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }

            return newNode;
        }
    }

    public static final class AddVertexByObjectUntypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final VertexFrame newVertex) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                case IN:
                    thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    break;
                case OUT:
                    thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }

            return newVertex;
        }
    }

    public static final class AddVertexByObjectTypedEdgeInterceptor {

        @RuntimeType
        public static Object addVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final VertexFrame newVertex, @RuntimeType @Argument(1) final ClassInitializer edgeType) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    thiz.getGraph().addFramedEdge(null, newVertex, thiz, label, edgeType);
                    thiz.getGraph().addFramedEdge(null, thiz, newVertex, label, edgeType);
                    break;
                case IN:
                    thiz.getGraph().addFramedEdge(null, newVertex, thiz, label, edgeType);
                    break;
                case OUT:
                    thiz.getGraph().addFramedEdge(null, thiz, newVertex, label, edgeType);
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }

            return newVertex;
        }
    }

    public static final class SetVertexInterceptor {

        @RuntimeType
        public static void setVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final Iterable vertexSet) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();


            switch (direction) {
                case BOTH:
                    for (final EdgeFrame existingEdge : thiz.bothE(label))
                        existingEdge.remove();
                    for (final VertexFrame newVertex : (Iterable<? extends VertexFrame>) vertexSet) {
                        thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                        thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    }
                    break;
                case IN:
                    for (final EdgeFrame existingEdge : thiz.inE(label))
                        existingEdge.remove();
                    for (final VertexFrame newVertex : (Iterable<? extends VertexFrame>) vertexSet)
                        thiz.getGraph().addFramedEdge(newVertex, thiz, label);
                    break;
                case OUT:
                    for (final EdgeFrame existingEdge : thiz.outE(label))
                        existingEdge.remove();
                    for (final VertexFrame newVertex : (Iterable<? extends VertexFrame>) vertexSet)
                        thiz.getGraph().addFramedEdge(thiz, newVertex, label);
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }

    public static final class RemoveVertexInterceptor {

        @RuntimeType
        public static void removeVertex(@This final VertexFrame thiz, @Origin final Method method, @RuntimeType @Argument(0) final VertexFrame removeVertex) {
            assert thiz instanceof CachesReflection;
            final Adjacency annotation = ((CachesReflection) thiz).getReflectionCache().getAnnotation(method, Adjacency.class);
            final Direction direction = annotation.direction();
            final String label = annotation.label();

            switch (direction) {
                case BOTH:
                    for (final EdgeFrame edge : thiz.bothE(label))
                        if (null == removeVertex || edge.outV().next().getId().equals(removeVertex.getId()) || edge.inV().next().getId().equals(removeVertex.getId()))
                            edge.remove();
                    break;
                case IN:
                    for (final EdgeFrame edge : thiz.inE(label))
                        if (null == removeVertex || edge.outV().next().getId().equals(removeVertex.getId()))
                            edge.remove();
                    break;
                case OUT:
                    for (final EdgeFrame edge : thiz.outE(label))
                        if (null == removeVertex || edge.inV().next().getId().equals(removeVertex.getId()))
                            edge.remove();
                    break;
                default:
                    throw new IllegalStateException(method.getName() + " is annotated with a direction other than BOTH, IN, or OUT.");
            }
        }
    }
}
