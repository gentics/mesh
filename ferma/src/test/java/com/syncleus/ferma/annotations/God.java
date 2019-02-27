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

import com.syncleus.ferma.*;
import com.tinkerpop.blueprints.Direction;

public interface God extends VertexFrame {
    static final ClassInitializer<God> DEFAULT_INITIALIZER = new DefaultClassInitializer(God.class);

    @Property("name")
    String getName();

    @Property("name")
    void setName(String newName);

    @Property("name")
    void removeName();

    @Property("age")
    Integer getAge();

    @Property("type")
    String getType();

    @Adjacency(label = "father", direction = Direction.IN)
    Iterable<? extends God> getSons();

    @Adjacency(label = "father", direction = Direction.IN)
    God getSon();

    @Adjacency(label = "father", direction = Direction.IN)
    <N extends God> Iterable<? extends N> getSons(Class<? extends N> type);

    @Adjacency(label = "father", direction = Direction.OUT)
    <N extends God> Iterable<? extends N> getParents();

    @Adjacency(label = "father", direction = Direction.IN)
    <N extends God> N getSon(Class<? extends N> type);

    @Adjacency(label = "father", direction = Direction.IN)
    <N extends God> N addSon(ClassInitializer<? extends N> type);

    @Adjacency(label = "father", direction = Direction.IN)
    <N extends God> N addSon(ClassInitializer<? extends N> type, ClassInitializer<? extends FatherEdge> edge);

    @Adjacency(label = "father", direction = Direction.IN)
    God addSon(God son);

    @Adjacency(label = "father", direction = Direction.IN)
    VertexFrame addSon();

    @Adjacency(label = "father", direction = Direction.IN)
    God addSon(God son, ClassInitializer<? extends FatherEdge> edge);

    @Adjacency(label = "father", direction = Direction.IN)
    void setSons(Iterable<? extends God> vertexSet);

    @Adjacency(label = "father", direction = Direction.IN)
    void removeSon(God son);

    @Incidence(label = "father", direction = Direction.IN)
    Iterable<? extends EdgeFrame> getSonEdges();

    @Incidence(label = "father", direction = Direction.IN)
    <N extends FatherEdge> Iterable<? extends N> getSonEdges(Class<? extends N> type);

    @Incidence(label = "father", direction = Direction.IN)
    EdgeFrame getSonEdge();

    @Incidence(label = "father", direction = Direction.IN)
    <N extends FatherEdge> N getSonEdge(Class<? extends N> type);

    @Incidence(label = "father", direction = Direction.IN)
    void removeSonEdge(FatherEdge edge);
}
