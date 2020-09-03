package com.gentics.mesh.assertj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.assertj.impl.ProjectResponseAssert;
import com.gentics.mesh.core.data.CreatorTrackingVertex;
import com.gentics.mesh.core.data.EditorTrackingVertex;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;

/**
 * Abstract AssertJ class for various rest model asserter implementations. (e.g: {@link ProjectResponseAssert}).
 *
 * @param <S>
 * @param <A>
 */
public class AbstractMeshAssert<S extends AbstractMeshAssert<S, A>, A> extends AbstractAssert<S, A> {

	protected AbstractMeshAssert(A actual, Class<?> selfType) {
		super(actual, selfType);
	}

	public void assertGenericNode(MeshCoreVertex<?> node, AbstractGenericRestResponse model) {
		assertNotNull(node);
		assertNotNull(model);
		assertNotNull("UUID field was not set in the rest response.", model.getUuid());
		assertEquals("The uuids should not be different", node.getUuid(), model.getUuid());
		assertNotNull("Permissions field was not set in the rest response.", model.getPermissions());
		assertNotNull("Creator field was not set in the rest response.", model.getCreator());
		assertNotNull("Editor field was not set in the rest response.", model.getEditor());

		if (node instanceof EditorTrackingVertex) {
			EditorTrackingVertex editedNode = (EditorTrackingVertex) node;
			assertNotNull("The editor of the graph node was not set.", editedNode.getEditor());
			assertEquals(editedNode.getEditor().getFirstname(), model.getEditor().getFirstName());
			assertEquals(editedNode.getEditor().getLastname(), model.getEditor().getLastName());
			assertEquals(editedNode.getEditor().getUuid(), model.getEditor().getUuid());
		}
		if (node instanceof CreatorTrackingVertex) {
			CreatorTrackingVertex createdNode = (CreatorTrackingVertex) node;
			assertEquals(createdNode.getCreator().getFirstname(), model.getCreator().getFirstName());
			assertEquals(createdNode.getCreator().getLastname(), model.getCreator().getLastName());
			assertEquals(createdNode.getCreator().getUuid(), model.getCreator().getUuid());
		}
	}
}
