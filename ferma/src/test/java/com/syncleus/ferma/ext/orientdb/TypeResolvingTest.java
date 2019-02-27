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

import org.junit.Test;

import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.ext.orientdb.model.IJob;
import com.syncleus.ferma.ext.orientdb.model.Job;
import com.syncleus.ferma.tx.Tx;

public class TypeResolvingTest extends AbstractOrientDBTest {

	@Test
	public void testCasting() {
		try (Tx tx = graph.tx()) {
			Job jobCTO = tx.getGraph().addFramedVertex(Job.class);
			jobCTO.setName("Chief Technology Officer");

			VertexFrame frame = tx.getGraph().v().has(Job.class).next();
			System.out.println(frame.getClass().getName());

			IJob job = tx.getGraph().v().has(Job.class).nextOrDefaultExplicit(Job.class, null);
			System.out.println(job.getName());
		}
	}

}
