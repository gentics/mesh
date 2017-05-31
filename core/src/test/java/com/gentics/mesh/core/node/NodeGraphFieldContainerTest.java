package com.gentics.mesh.core.node;

import static com.gentics.mesh.test.TestSize.FULL;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.orientechnologies.orient.core.storage.ORecordDuplicatedException;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = false)
public class NodeGraphFieldContainerTest extends AbstractMeshTest {

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingWebRootPath() {
		try (Tx tx = db().tx()) {
			NodeGraphFieldContainer containerA = Database.getThreadLocalGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldContainer containerB = Database.getThreadLocalGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			containerA.getElement().setProperty(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY, "test");
			containerB.getElement().setProperty(NodeGraphFieldContainerImpl.WEBROOT_PROPERTY_KEY, "test");
			tx.success();
		}
	}

	@Test(expected = ORecordDuplicatedException.class)
	public void testConflictingPublishWebRootPath() {
		try (Tx tx = db().tx()) {
			NodeGraphFieldContainer containerA = Database.getThreadLocalGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldContainer containerB = Database.getThreadLocalGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			containerA.getElement().setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, "test");
			containerB.getElement().setProperty(NodeGraphFieldContainerImpl.PUBLISHED_WEBROOT_PROPERTY_KEY, "test");
			tx.success();
		}
	}
}
