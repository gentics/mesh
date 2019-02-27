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
/**
 * This product currently only contains code developed by authors
 * of specific components, as identified by the source code files.
 *
 * Since product implements StAX API, it has dependencies to StAX API
 * classes.
 *
 * For additional credits (generally to people who reported problems)
 * see CREDITS file.
 */
package com.syncleus.ferma.ext.orientdb;

import static com.syncleus.ferma.ext.orientdb.util.TestUtils.run;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.orientechnologies.orient.core.exception.OConcurrentModificationException;
import com.syncleus.ferma.ext.orientdb.model.Person;
import com.syncleus.ferma.tx.Tx;

public class TxTest extends AbstractOrientDBTest {

	private Person p;

	@Test
	public void testTxConflictHandling() throws InterruptedException, BrokenBarrierException, TimeoutException {
		// Test creation of user in current thread
		int nFriendsBefore;
		try (Tx tx = graph.tx()) {
			p = addPersonWithFriends(tx.getGraph(), "Person2");
			manipulatePerson(tx.getGraph(), p);
			tx.success();
			nFriendsBefore = p.getFriends().size();
		}

		CyclicBarrier b = new CyclicBarrier(3);
		addFriendToPersonInThread(p, b);
		addFriendToPersonInThread(p, b);

		// Wait until both threads have started their transactions
		b.await();
		Thread.sleep(2000);
		try (Tx tx = graph.tx()) {
			// Reload the person in a fresh transaction
			p = tx.getGraph().getFramedVertexExplicit(Person.class, p.getId());
			int nFriendsAfter = p.getFriends().size();
			assertEquals(nFriendsBefore + 2, nFriendsAfter);
		}

	}

	/**
	 * Add a new friend to the provided person and wait on the barrier before finishing the transaction. This is useful if you want to produce a
	 * {@link OConcurrentModificationException}
	 * 
	 * @param p
	 * @param b
	 */
	private void addFriendToPersonInThread(Person p, CyclicBarrier b) {
		run(() -> {
			for (int retry = 0; retry < 10; retry++) {

				System.out.println("Try: " + retry);
				boolean doRetry = false;
				try (Tx tx = graph.tx()) {
					// Reload the person since the person vertex could have been altered by another transaction.
					Person p1 = tx.getGraph().getFramedVertexExplicit(Person.class, p.getId());

					addFriend(tx.getGraph(), p1);
					tx.success();
					if (retry == 0) {
						try {
							b.await();
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				} catch (OConcurrentModificationException e) {
					System.out.println("Got modification exception on run {" + retry + "}. Invoking retry.");
					doRetry = true;
				}
				if (!doRetry) {
					break;
				}
				System.out.println("Retry");
			}
		});
	}
}
