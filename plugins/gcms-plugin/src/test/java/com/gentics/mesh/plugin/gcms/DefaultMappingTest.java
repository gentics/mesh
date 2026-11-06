package com.gentics.mesh.plugin.gcms;

import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.junit.Test;

import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

/**
 * Test for the default mapping
 */
public class DefaultMappingTest extends AbstractGCMSPluginTest {

	@Test
	public void testDefaultMapping() throws IOException {
		String token = getToken("testuser", "First", "Last", "testuser@nowhere", Arrays.asList("Group1", "Group2"));
		MeshRestClient client = server.client();
		client.setAPIKey(token);
		UserResponse userResponse = call(() -> client.me());
		assertThat(userResponse.getUsername()).as("Username").isEqualTo("testuser");
		assertThat(userResponse.getGroups().stream().map(GroupReference::getName).collect(Collectors.toList()))
				.as("Groups").containsOnly("Group1", "Group2");
	}
}
