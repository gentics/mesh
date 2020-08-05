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
package com.gentics.mesh.core.db;

/**
 * Interface which can be used for custom transaction factories in 
 * order to provide various ways of executing transaction handlers.
 */
public interface TxFactory {

    /**
     * Return a new autoclosable transaction handler. This object should be used within a try-with-resource block.
     * 
     * <pre>
     * {
     * 	&#64;code
     * 	try(Tx tx = db.tx()) {
     * 	  // interact with graph db here
     *  }
     * }
     * </pre>
     * 
     * @return Created transaction
     */
    Tx tx();
    
    /**
     * Execute the txHandler within the scope of a transaction and call 
     * the result handler once the transaction handler code has finished.
     * 
     * @param txHandler
     *            Handler that will be executed within the scope of the transaction.
     * @return Object which was returned by the handler
     */
    <T> T tx(TxAction<T> txHandler);

    /**
     * Execute the txHandler within the scope of a transaction.
     * 
     * @param txHandler
     *            Handler that will be executed within the scope of the transaction.
     */
    default void tx(TxAction0 txHandler) {
        tx((tx) -> {
            txHandler.handle();
        });
    }

    /**
     * Execute the txHandler within the scope of a transaction.
     * 
     * @param txHandler
     *            Handler that will be executed within the scope of the transaction.
     * @return Result of the handler
     */
    default <T> T tx(TxAction1<T> txHandler) {
        return tx((tx) -> {
            return txHandler.handle();
        });
    }

    /**
     * Execute the txHandler within the scope of a transaction.
     * 
     * @param txHandler
     *            Handler that will be executed within the scope of the transaction.
     */
    default void tx(TxAction2 txHandler) {
        tx((tx) -> {
            txHandler.handle(tx);
            return null;
        });
    }

}
