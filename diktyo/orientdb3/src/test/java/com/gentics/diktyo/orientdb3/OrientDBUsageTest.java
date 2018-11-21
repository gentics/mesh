package com.gentics.diktyo.orientdb3;

import static org.junit.Assert.assertNotNull;

import org.apache.commons.configuration.BaseConfiguration;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraph;
import org.apache.tinkerpop.gremlin.orientdb.OrientGraphFactory;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.junit.Test;

import com.orientechnologies.orient.core.index.OIndex;
import com.orientechnologies.orient.core.index.OIndexManager;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OProperty;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;

public class OrientDBUsageTest {

	@Test
	public void testOrientDB() {

		try (OrientGraphFactory graph = new OrientGraphFactory()) {
			graph.setupPool(10, 100);

			OSchema schema = graph.getNoTx().getRawDatabase().getMetadata().getSchema();

			Configuration idxConfig = new BaseConfiguration();
			idxConfig.setProperty("type", "UNIQUE");
			idxConfig.setProperty("keytype", OType.STRING);
			graph.getNoTx().createVertexIndex("name", "Job", idxConfig);

			Configuration idx2Config = new BaseConfiguration();
			idxConfig.setProperty("type", "UNIQUE");
			idxConfig.setProperty("keytype", OType.STRING);
			OIndexManager indexManager = graph.getNoTx().getRawDatabase().getMetadata().getIndexManager();

			graph.getNoTx().createEdgeIndex("hasjobidx", "HAS_JOB", idx2Config);

			OClass clazz = schema.getClass("Employee");
			OProperty prop = clazz.getProperty("address");
			ODocument metadata = new ODocument().field("ignoreNullValues", true);
			prop.createIndex(OClass.INDEX_TYPE.NOTUNIQUE, metadata);

			try (OrientGraph tx = graph.getTx()) {
				for (int e = 0; e < 100_000; e++) {
					Vertex v = tx.addVertex("Person");
					v.property("name", "blab" + e);
					Vertex v2 = tx.addVertex("Job");
					v2.property("name", "blub" + e);
				}
				tx.commit();
			}

			try (OrientGraph tx = graph.getTx()) {

				OIndex<?> index = indexManager.getClassIndex("Job", "Job.name");
				assertNotNull(index);
				System.out.println("FK: " + index.getFirstKey().getClass().getName() + " val: " + index.getFirstKey());
				time(() -> {
					System.out.println("Persons: " + tx.traversal().V().hasLabel("Person").count().next());
				});
			}
		}
	}

	// time(() -> {
	// System.out.println("Count: " + tx.traversal().V().has("name", "blub99").count().next());
	// });
	// time(() -> {
	// System.out.println("Jobs: " + tx.getRawDatabase().getMetadata().getSchema().getClass("Job").count());
	// System.out.println("Jobs: " + tx.traversal().V().has("@class", "Job").count().next());
	// });
	// time(() -> {
	// System.out.println("Persons: " + tx.traversal().V().has("@class", "Person").count().next());
	// });

	public void time(Runnable r) {
		long start = System.currentTimeMillis();
		r.run();
		long dur = System.currentTimeMillis() - start;
		System.out.println("Duration: " + dur);
	}
}
