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

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.orientechnologies.orient.core.exception.OSchemaException;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.tx.TxAction;
import com.syncleus.ferma.tx.TxFactory;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public interface OrientTransactionFactory extends TxFactory {

	/**
	 * Return the configured orientdb graph factory from which transaction are created.
	 * 
	 * @return Underlying factory
	 */
	OrientGraphFactory getFactory();

	/**
	 * Return the configured type resolver.
	 * 
	 * @return Configured type resolver
	 */
	OrientDBTypeResolver getTypeResolver();

	/**
	 * Return the configured amount of maximum retries.
	 * 
	 * @return Configured value
	 */
	int getMaxRetry();

	@Override
	default Tx tx() {
		return new OrientDBTx(getFactory(), getTypeResolver());
	}

	@Override
	default <T> T tx(TxAction<T> txAction) {
		/**
		 * OrientDB uses the MVCC pattern which requires a retry of the code that manipulates the graph in cases where for example an
		 * {@link OConcurrentModificationException} is thrown.
		 */
		T handlerResult = null;
		boolean handlerFinished = false;
		for (int retry = 0; retry < getMaxRetry(); retry++) {

			try (Tx tx = tx()) {
				handlerResult = txAction.handle(tx);
				handlerFinished = true;
				tx.success();
			} catch (OSchemaException e) {
				throw new RuntimeException("", e);
			} catch (OConcurrentModificationException e) {
				try {
					// Delay the retry by 50ms to give the other transaction a chance to finish
					Thread.sleep(50 + (retry * 5));
				} catch (InterruptedException e1) {
					e1.printStackTrace();
				}
				// Reset previous result
				handlerFinished = false;
				handlerResult = null;
			} catch (ORecordDuplicatedException e) {
				throw new RuntimeException("Duplicate record detected.", e);
			} catch (RuntimeException e) {
				throw e;
			} catch (Exception e) {
				throw new RuntimeException("Transaction error", e);
			}
			if (handlerFinished) {
				return handlerResult;
			}
		}
		throw new RuntimeException("Retry limit {" + getMaxRetry() + "} for trx exceeded");
	}

}
