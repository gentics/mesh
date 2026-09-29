package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class IndexResetTest extends AbstractNodeSearchEndpointTest {

	public IndexResetTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testRandomIndexBeingDropped() throws Exception {
		Set<String> oldIndices = searchProvider().listIndices().blockingGet();
		long deleted = oldIndices.stream()
			.filter(index -> index.startsWith("node-"))
			.peek(index -> {
				ElasticsearchClient<JsonObject> client = searchProvider().getClient();
				try {
					client.deleteIndex(options().getSearchOptions().getPrefix() + index).sync();
					System.out.println("Index dropped: " + index);
				} catch (HttpErrorException e) {
					throw new IllegalStateException(e);
				}
			}).count();
		assertThat(deleted).as("Number of preliminary dropped indices").isGreaterThan(0);

		recreateIndices();
		Set<String> currentIndices = searchProvider().listIndices().blockingGet();
		assertThat(currentIndices).as("Recreated indices").containsExactlyInAnyOrderElementsOf(oldIndices);
	}
}
