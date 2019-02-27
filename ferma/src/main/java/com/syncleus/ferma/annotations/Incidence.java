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
package com.syncleus.ferma.annotations;

import com.tinkerpop.blueprints.Direction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Incidences annotate getters and adders to represent a Vertex incident to an Edge. This annotation extends the
 * TinkerPop built-in Incidence annotation. It allows type arguments to be passed into the annotated method. This
 * ensures the returned type is of the specified type in the argument. The following method signatures are valid.
 *
 *
 * T add*(Class&lt;T&gt; type)
 * T get*(Class&lt;T&gt; type)
 *
 * When annotating a get* class it ensures it only returns nodes of the specified type (including sub-classes). Any
 * Nodes which are not of this type will not be returned.
 *
 * @since 2.0.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Incidence {

    /**
     * The labels of the edges that are incident to the vertex.
     *
     * @return the edge label
     * @since 2.0.0
     */
    String label();

    /**
     * The direction of the edges.
     *
     * @return the edge direction
     * @since 2.0.0
     */
    Direction direction() default Direction.OUT;
}
