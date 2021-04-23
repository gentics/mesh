package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;

public class ClusterTortureAllNodesKilled extends AbstractClusterTortureTest {
	
	/**
	 * Kill all nodes during sync process
	 * 
	 * @throws Exception
	 */
	// Fails as of OrientDB 3.1.11.
	@Test
	public void testAllKilled() throws Exception {
		torture((serverA, serverB, contentSchema) -> {
			String schemaUuid = contentSchema.getUuid();
			
			new Thread(() -> {
					SchemaUpdateRequest schemaUpdateRequest = contentSchema.toUpdateRequest();
					schemaUpdateRequest.removeField("teaser");
					schemaUpdateRequest.addField(new DateFieldSchemaImpl().setName("teaser"), "content");
					
					call(() -> serverA.client().updateSchema(schemaUuid, schemaUpdateRequest));
			}).run();
			
			Thread.sleep(5000);
			
			new Thread(() -> {
					serverB.killHardContainer();
					serverA.killHardContainer();
			}).run();
		});
	}
}
