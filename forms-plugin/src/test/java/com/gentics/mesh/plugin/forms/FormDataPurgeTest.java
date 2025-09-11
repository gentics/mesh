package com.gentics.mesh.plugin.forms;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.gentics.mesh.core.rest.node.NodeResponse;

import okhttp3.Request;

/**
 * Test cases for purging stale data nodes
 */
public class FormDataPurgeTest extends AbstractFormPostDataTest {
	/**
	 * Number of forms
	 */
	protected final static int NUM_FORMS = 5;

	/**
	 * Number of data nodes per form
	 */
	protected final static int NUM_DATA = 20;

	@Test
	public void testPurge() throws IOException, InterruptedException {
		List<String> formUuids = new ArrayList<>();
		for (int i = 0; i < NUM_FORMS; i++) {
			formUuids.add(createForm("inputfield.json"));
		}

		// post some data for each form
		for (String formUuid : formUuids) {
			for (int i = 0; i < NUM_DATA; i++) {
				boolean empty = i % 2 == 0;
				submit(formUuid, true, empty, "input1", empty ? "" : "data %d".formatted(i));
			}
		}

		Map<String, Pair<Long, Long>> dataNodesPerForm = new HashMap<>();

		for (String formUuid : formUuids) {
			List<NodeResponse> nodeList = getFormDataNodes(formUuid);
			long published = nodeList.stream().flatMap(node -> node.getAvailableLanguages().values().stream())
					.filter(status -> status.isPublished()).count();
			long notPublished = nodeList.stream().flatMap(node -> node.getAvailableLanguages().values().stream())
					.filter(status -> !status.isPublished()).count();
			assertThat(published).as("Number of published data nodes for form %s".formatted(formUuid)).isGreaterThan(0);
			assertThat(notPublished).as("Number of not published data nodes for form %s".formatted(formUuid)).isGreaterThan(0);
			assertThat(published + notPublished).as("Total number of data nodes for form %s".formatted(formUuid)).isEqualTo(NUM_DATA);

			dataNodesPerForm.put(formUuid, Pair.of(published, notPublished));
		}

		Thread.sleep(2000);

		doRequest(new Request.Builder()
				.url(projectUrl("/test/purge/1?perPage=3"))
				.get()
				.build(), true, true, JsonNode.class);

		// we expect all published nodes to still exist, but the unpublished nodes to have gone
		for (String formUuid : formUuids) {
			List<NodeResponse> nodeList = getFormDataNodes(formUuid);
			long published = nodeList.stream().flatMap(node -> node.getAvailableLanguages().values().stream())
					.filter(status -> status.isPublished()).count();
			long notPublished = nodeList.stream().flatMap(node -> node.getAvailableLanguages().values().stream())
					.filter(status -> !status.isPublished()).count();
			assertThat(published).as("Number of published data nodes for form %s after purge".formatted(formUuid)).isEqualTo(dataNodesPerForm.get(formUuid).getLeft());
			assertThat(notPublished).as("Number of not published data nodes for form %s after purge".formatted(formUuid)).isEqualTo(0);
		}
	}
}
