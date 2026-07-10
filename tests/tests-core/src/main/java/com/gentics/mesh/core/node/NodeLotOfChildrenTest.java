package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeLotOfChildrenTest extends AbstractMassiveNodeLoadTest {

	@Test
	public void testReadAll() {
		NodeListResponse nodeList = call(() -> client().findNodeChildren(PROJECT_NAME, parentFolderUuid, new VersioningParametersImpl().draft()));
		assertEquals("The subnode did not contain the created node", numOfNodesPerLevel, nodeList.getData().size());
	}
}
