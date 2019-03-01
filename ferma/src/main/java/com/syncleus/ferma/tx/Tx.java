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
package com.syncleus.ferma.tx;

import com.syncleus.ferma.FramedTransactionalGraph;

/**
 * A {@link Tx} is an interface for autoclosable transactions.
 */
public interface Tx extends AutoCloseable {

    /**
     * Thread local that is used to store references to the used graph.
     */
    public static ThreadLocal<Tx> threadLocalGraph = new ThreadLocal<>();

    /**
     * Set the nested active transaction for the current thread.
     * 
     * @param tx
     *            Transaction
     */
    public static void setActive(Tx tx) {
        Tx.threadLocalGraph.set(tx);
    }

    /**
     * Return the current active graph. A transaction should be the only place where this threadlocal is updated.
     * 
     * @return Currently active transaction
     */
    public static Tx getActive() {
        return Tx.threadLocalGraph.get();
    }

    /**
     * Mark the transaction as succeeded. The autoclosable will invoke a commit when completing.
     */
    void success();

    /**
     * Mark the transaction as failed. The autoclosable will invoke a rollback when completing.
     */
    void failure();

    /**
     * Return the framed graph that is bound to the transaction.
     * 
     * @return Graph which is bound to the transaction.
     */
    FramedTransactionalGraph getGraph();

    /**
     * Invoke rollback or commit when closing the autoclosable. By default a rollback will be invoked.
     */
    @Override
    void close();

    /**
     * Add new isolated vertex to the graph.
     * 
     * @param <T>
     *            The type used to frame the element.
     * @param kind
     *            The kind of the vertex
     * @return The framed vertex
     * 
     */
    default <T> T addVertex(Class<T> kind) {
        return getGraph().addFramedVertex(kind);
    }

}
