package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.request.Bulkable;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.DeleteDocumentRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.search.TrackingSearchProvider;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.json.JsonObject;

public class DummySearchProviderAssert extends AbstractAssert<DummySearchProviderAssert, TrackingSearchProvider> {

	public DummySearchProviderAssert(TrackingSearchProvider actual) {
		super(actual, DummySearchProviderAssert.class);
	}

	public DummySearchProviderAssert recordedStoreEvents(int count) {
		isNotNull();
		String info = actual.getStoreEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of store events. Found events: {\n" + info + "\n}", count, actual
				.getStoreEvents().size());
		return this;
	}

	public DummySearchProviderAssert recordedDeleteEvents(int count) {
		isNotNull();
		String info = actual.getDeleteEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		int found = actual.getDeleteEvents().size();
		if (found != count) {
			failWithMessage("The search provider did not record the correct amount {%s} of delete events. Found {%s} events: {\n" + info + "\n}",
					count, found);
		}
		return this;
	}

	public DummySearchProviderAssert hasNoStoreEvents() {
		return recordedStoreEvents(0);
	}

	/**
	 * Verify that the search provider recorded the given store event.
	 * 
	 * @param indexName
	 * @param indexType
	 * @param documentId
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasStore(String indexName, String documentId) {
		String key = indexName + "-" + documentId;
		boolean hasKey = actual.getStoreEvents().containsKey(key);
		if (!hasKey) {
			for (String event : actual.getStoreEvents().keySet()) {
				System.out.println("Recorded store event: " + event);
			}
		}
		assertTrue("The store event could not be found. {" + indexName + "} {" + documentId + "}", hasKey);
		return this;
	}

	/**
	 * Verify that the search provider recorded the given create event.
	 * 
	 * @param indexName
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasCreate(String indexName) {
		JsonObject indexInfo = actual.getCreateIndexEvents().get(indexName);
		if (indexInfo == null) {
			for (String event : actual.getCreateIndexEvents().keySet()) {
				System.out.println("Recorded create event: " + event);
			}
			fail("The create event could not be found. {" + indexName + "}");
		}
		return this;
	}

	/**
	 * Verify that the search provider recorded the given delete event.
	 * 
	 * @param indexName
	 * @param documentId
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasDelete(String indexName, String documentId) {
		String key = indexName + "-" + documentId;
		boolean hasKey = actual.getDeleteEvents().contains(key);
		if (!hasKey) {
			for (String event : actual.getDeleteEvents()) {
				System.out.println("Recorded delete event: " + event);
			}
		}
		assertTrue("The delete event could not be found. {" + indexName + "} - {" + documentId + "}", hasKey);
		return this;
	}

	/**
	 * Verify that the search provider recorded the given drop index event.
	 * 
	 * @param composeIndexName
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasDrop(String indexName) {
		boolean hasDrop = actual.getDropIndexEvents().contains(indexName);
		if (!hasDrop) {
			for (String event : actual.getDropIndexEvents()) {
				System.out.println("Recorded drop event: " + event);
			}
		}
		assertTrue("The drop index event could not be found. {" + indexName + "}", hasDrop);
		return this;
	}

	/**
	 * Assert that the correct count of events was registered.
	 * 
	 * @param storeEvents
	 * @param updateEvents
	 * @param deleteEvents
	 * @param dropIndexEvents
	 * @param createIndexEvents
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasEvents(long storeEvents, long updateEvents, long deleteEvents, long dropIndexEvents, long createIndexEvents) {
		String storeInfo = actual.getStoreEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of store events. Found events: {\n" + storeInfo + "\n}", storeEvents,
				actual.getStoreEvents().size());

		String updateInfo = actual.getUpdateEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of update events. Found events: {\n" + updateInfo + "\n}", updateEvents,
			actual.getUpdateEvents().size());

		String deleteInfo = actual.getDeleteEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of delete events. Found events: {\n" + deleteInfo + "\n}", deleteEvents,
				actual.getDeleteEvents().size());

		String dropInfo = actual.getDropIndexEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of drop index events. Found events: {\n" + dropInfo + "\n}",
				dropIndexEvents, actual.getDropIndexEvents().size());

		String createInfo = actual.getCreateIndexEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of create index events. Found events: {\n" + createInfo + "\n}",
				createIndexEvents, actual.getCreateIndexEvents().size());

		return this;
	}

	/**
	 * Assert that the node was stored in the index for given languages and DRAFT and PUBLISHED versions
	 * 
	 * @param node
	 * @param project
	 * @param branch
	 * @param languages
	 * @return Fluent API
	 */
	public DummySearchProviderAssert storedAllContainers(Node node, Project project, Branch branch, String... languages) {
		for (ContainerType type : Arrays.asList(DRAFT, PUBLISHED)) {
			for (String lang : languages) {
				String projectUuid = project.getUuid();
				String branchUuid = branch.getUuid();
				String schemaVersionUuid = node.getSchemaContainer().getLatestVersion().getUuid();
				assertThat(actual).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid, schemaVersionUuid, type, node.getSchemaContainer().getLatestVersion().getMicroschemaVersionHash(branch)),
						NodeGraphFieldContainer.composeDocumentId(node.getUuid(), lang));
			}
		}
		return this;
	}

	/**
	 * Assert that the tag family was stored in the index.
	 * 
	 * @param tag
	 * @return Fluent API
	 */
	public DummySearchProviderAssert stored(Tag tag) {
		assertThat(actual).hasStore(Tag.composeIndexName(tag.getProject().getUuid()), Tag.composeDocumentId(tag.getUuid()));
		return this;
	}

	/**
	 * Assert that the tag family was stored in the index.
	 * 
	 * @param tagfamily
	 * @return Fluent API
	 */
	public DummySearchProviderAssert stored(TagFamily tagfamily) {
		assertThat(actual).hasStore(TagFamily.composeIndexName(tagfamily.getProject().getUuid()), TagFamily.composeDocumentId(tagfamily.getUuid()));
		return this;
	}

	/**
	 * Assert that no drop event was recorded.
	 * 
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasNoDropEvents() {
		assertThat(actual.getDropIndexEvents()).isEmpty();
		return this;
	}

	/**
	 * Assert that there is a node document delete event for every node document create event.
	 * @return
	 */
	public DummySearchProviderAssert hasSymmetricNodeRequests() {
		List<Tuple<CreateDocumentRequest, DeleteDocumentRequest>> requests = actual.getBulkRequests()
			.stream()
			.filter(this::isNodeRequest)
			.collect(toPairs(CreateDocumentRequest.class, DeleteDocumentRequest.class));
		requests.forEach(this::assertMatching);
		return this;
	}

	private boolean isNodeRequest(Bulkable request) {
		if (request instanceof CreateDocumentRequest) {
			CreateDocumentRequest req = (CreateDocumentRequest) request;
			return req.getIndex().startsWith("node");
		} else if (request instanceof DeleteDocumentRequest) {
			DeleteDocumentRequest req = (DeleteDocumentRequest) request;
			return req.getIndex().startsWith("node");
		} else {
			return false;
		}
	}

	private void assertMatching(Tuple<CreateDocumentRequest, DeleteDocumentRequest> requests) {
		String id1 = requests.v1().getId();
		String id2 = requests.v2().getId();
		assertEquals(String.format("Found non-matching pair:\n%s\n%s", id1, id2),
			id1, id2);
	}

	private <T, R1, R2> Collector<T, ?, List<Tuple<R1, R2>>> toPairs(Class<R1> r1Class, Class<R2> r2Class) {
		return Collectors.collectingAndThen(Collectors.toList(), list ->
			IntStream.iterate(0, i -> i + 2)
			.limit(list.size() / 2)
			.mapToObj(i ->
				Tuple.tuple(
					requireType(r1Class, list.get(i)),
					requireType(r2Class, list.get(i+1))
				)
			)
			.collect(Collectors.toList())
		);
	}

	private <T> T requireType(Class<T> clazz, Object obj) {
		if (clazz.isAssignableFrom(obj.getClass())) {
			return (T) obj;
		} else {
			throw new RuntimeException(String.format("Unexpected type. Required {%s}, but got {%s}", clazz.getSimpleName(), obj.getClass().getSimpleName()));
		}
	}
}
