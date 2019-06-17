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
package com.syncleus.ferma;

import java.util.Iterator;
import java.util.List;

public class Person extends AbstractVertexFrame {
    static final ClassInitializer<Person> DEFAULT_INITIALIZER = new DefaultClassInitializer(Person.class);

    public String getName() {
        return getProperty("name");
    }

    public void setName(final String name) {
        setProperty("name", name);
    }

    public Iterator<Person> getKnows() {

        return out("knows").frame(Person.class).iterator();
    }

    public Iterator<Person> getKnowsExplicit() {
        return out("knows").frame(Person.class).iterator();
    }

    public List<? extends Knows> getKnowsList() {
        return outE("knows").toList(Knows.class);
    }

    public List<? extends Knows> getKnowsListExplicit() {
        return outE("knows").toListExplicit(Knows.class);
    }

    public List<? extends Person> getKnowsCollectionVertices() {
        return out("knows").toList(Person.class);
    }

    public List<? extends Person> getKnowsCollectionVerticesExplicit() {
        return out("knows").toListExplicit(Person.class);
    }

    public Person getFirst() {
        return out("knows").next(Person.class);
    }

    public Person getFirstExplicit() {
        return out("knows").nextExplicit(Person.class);
    }

    public Knows addKnows(final Person friend) {
        return addFramedEdge("knows", friend, Knows.DEFAULT_INITIALIZER);
    }

    public Knows addKnowsExplicit(final Person friend) {
        return addFramedEdgeExplicit("knows", friend, Knows.DEFAULT_INITIALIZER);
    }

}
