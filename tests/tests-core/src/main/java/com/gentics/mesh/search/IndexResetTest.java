package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.slf4j.Logger;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.mock.MockingLoggerRule;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshCoreOptionChanger;
import com.gentics.mesh.test.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true, optionChanger = MeshCoreOptionChanger.DEBUG_LOG)
public class IndexResetTest extends AbstractNodeSearchEndpointTest {

	// @Rule won't work here because of an explicit Mesh init in AbstractNodeSearchEndpointTest's ctor
	public static MockingLoggerRule rule;

	static {
		rule = new MockingLoggerRule();
		rule.initialize();
	}

	protected Logger logger = rule.getLogger(ElasticSearchProvider.class.getName());

	public IndexResetTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testRandomIndexBeingDropped() throws Exception {
		rule.reset();
		Set<String> oldIndices = searchProvider().listIndices().blockingGet();
		Set<String> deleted = oldIndices.stream()
			.filter(index -> (index.hashCode() % 2) == 0)
			.peek(index -> {
				ElasticsearchClient<JsonObject> client = searchProvider().getClient();
				index = options().getSearchOptions().getPrefix() + index;
				try {
					client.deleteIndex(index).sync();
					System.out.println("Index dropped: " + index);
				} catch (HttpErrorException e) {
					throw new IllegalStateException(e);
				}
			}).collect(Collectors.toSet());
		assertThat(deleted.size()).as("Number of preliminary dropped indices").isGreaterThan(0);

		recreateIndices();
		Set<String> currentIndices = searchProvider().listIndices().blockingGet();
		assertThat(currentIndices).as("Recreated indices").containsExactlyInAnyOrderElementsOf(oldIndices);

		verify(logger).info("Sending index clear completed event");
		verify(logger, never()).warn(any());
		verify(logger, never()).error(any());
	}
}
