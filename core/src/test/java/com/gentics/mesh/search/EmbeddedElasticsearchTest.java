package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.PROJECT_AND_NODE, startServer = true, useElasticsearchContainer = false)
public class EmbeddedElasticsearchTest extends AbstractMeshTest {

	@Test
	public void testSimpleQuerySearch() throws IOException {
		String username = "testuser42a";
		try (Tx tx = tx()) {
			createUser(username);
		}

		String json = getESText("userWildcard.es");

		UserListResponse list = call(() -> client().searchUsers(json));
		assertEquals(1, list.getData().size());
		assertEquals("The found element is not the user we were looking for", username, list.getData().get(0).getUsername());

	}

}
