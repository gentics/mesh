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
 * Adjacencies annotate getters and adders to represent a Vertex adjacent to a Vertex. This annotation extends the
 * TinkerPop built-in Adjacency annotation. It allows type arguments to be passed into the annotated method. This
 * ensures the returned type is of the specified type in the argument. The following method signatures are valid.
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
public @interface Adjacency {

    /**
     * The label of the edges making the adjacency between the vertices.
     *
     * @return the edge label
     * @since 2.0.0
     */
    String label();

    /**
     * The edge direction of the adjacency.
     *
     * @return the direction of the edges composing the adjacency
     * @since 2.0.0
     */
    Direction direction() default Direction.OUT;
}
