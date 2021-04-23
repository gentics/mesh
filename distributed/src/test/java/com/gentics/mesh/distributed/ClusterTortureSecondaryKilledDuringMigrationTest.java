package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.Test;

import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.docker.MeshContainer;

/**
 * Cluster Torture: Kill the secondary node during the sync process caused by a massive node migration.
 * 
 * @author plyhun
 *
 */
public class ClusterTortureSecondaryKilledDuringMigrationTest extends AbstractClusterTortureTest {

	@Test
	public void testSecondaryKilledDuringMigration() throws Exception {		
		torture((a, b, c) -> {
			String schemaUuid = c.getUuid();
			b.stop();
			
			MeshContainer serverB1 = prepareSlave("dockerCluster" + clusterPostFix, "nodeB", b.getDataPathPostfix(), false, false, 1);
			serverB1.start();
			
			new Thread(() -> {
					SchemaUpdateRequest schemaUpdateRequest = c.toUpdateRequest();
					schemaUpdateRequest.removeField("teaser");
					
					// We make a new field of the same name different by type for a reason - there will be conflicts during the processing
					// of each node, that still have to be correctly processed and recovered.
					schemaUpdateRequest.addField(new DateFieldSchemaImpl().setName("teaser"), "content");
					
					call(() -> a.client().updateSchema(schemaUuid, schemaUpdateRequest));
			}).run();
			
			Thread.sleep(4000);
			serverB1.killHardContainer();
		});
	}
}
