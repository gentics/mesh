/**
 * Copyright 2004 - 2017 Syncleus, Inc.
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
package com.syncleus.ferma.ext.orientdb;

import org.junit.Test;

import com.syncleus.ferma.ext.orientdb.impl.OrientTransactionFactoryImpl;
import com.syncleus.ferma.ext.orientdb.model.Person;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.tx.TxFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

/**
 * Test the example code of the readme to ensure that it is working
 */
public class ExampleCodeTest {

    @Test
    public void testExample() {
        // Setup the orientdb graph factory from which the transaction factory will create transactions
        OrientGraphFactory graphFactory = new OrientGraphFactory("memory:tinkerpop").setupPool(4, 10);
        TxFactory graph = new OrientTransactionFactoryImpl(graphFactory, "com.syncleus.ferma.ext.orientdb.model");

        try (Tx tx = graph.tx()) {
            Person joe = tx.getGraph().addFramedVertex(Person.class);
            joe.setName("Joe");
            Person hugo = tx.getGraph().addFramedVertex(Person.class);
            hugo.setName("Hugo");

            // Both are mutal friends
            joe.addFriend(hugo);
            hugo.addFriend(joe);
            tx.success();
        }

        try (Tx tx = graph.tx()) {
            for (Person p : tx.getGraph().getFramedVerticesExplicit(Person.class)) {
                System.out.println("Found person with name: " + p.getName());
            }
        }

        String result = graph.tx((tx) -> {
            Person hugo = tx.getGraph().getFramedVertices("name", "Hugo", Person.class).iterator().next();
            StringBuffer sb = new StringBuffer();
            sb.append("Hugo's friends:");

            for (Person p : hugo.getFriends()) {
                sb.append(" " + p.getName());
            }
            return sb.toString();
        });
        System.out.println(result);
    }

}
