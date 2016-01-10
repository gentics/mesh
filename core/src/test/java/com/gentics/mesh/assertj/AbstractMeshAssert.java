package com.gentics.mesh.assertj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

public class AbstractMeshAssert<S extends AbstractMeshAssert<S, A>, A> extends AbstractAssert<S, A> {

	protected AbstractMeshAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}
	
	public void assertGenericNode(MeshCoreVertex<?, ?> node, AbstractGenericRestResponse model) {
		assertNotNull(node);
		assertNotNull(model);
		assertNotNull("UUID field was not set in the rest response.", model.getUuid());
		assertEquals("The uuids should not be different", node.getUuid(), model.getUuid());
		assertNotNull("Permissions field was not set in the rest response.", model.getPermissions());
		assertNotNull("Creator field was not set in the rest response.", model.getCreator());
		assertNotNull("Editor field was not set in the rest response.", model.getEditor());
		assertNotNull("The editor of the graph node was not set.", node.getEditor());
		assertEquals(node.getEditor().getUsername(), model.getEditor().getName());
		assertEquals(node.getEditor().getUuid(), model.getEditor().getUuid());
		assertEquals(node.getCreator().getUsername(), model.getCreator().getName());
		assertEquals(node.getCreator().getUuid(), model.getCreator().getUuid());
	}


}
