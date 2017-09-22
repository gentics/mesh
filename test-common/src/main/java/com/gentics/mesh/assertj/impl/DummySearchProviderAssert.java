package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.search.DummySearchProvider;

public class DummySearchProviderAssert extends AbstractAssert<DummySearchProviderAssert, DummySearchProvider> {

	public DummySearchProviderAssert(DummySearchProvider actual) {
		super(actual, DummySearchProviderAssert.class);
	}

	public DummySearchProviderAssert recordedStoreEvents(int count) {
		isNotNull();
		String info = actual.getStoreEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of store events. Found events: {\n" + info + "\n}", count,
				actual.getStoreEvents().size());
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
	public DummySearchProviderAssert hasStore(String indexName, String indexType, String documentId) {
		String key = indexName + "-" + indexType + "-" + documentId;
		boolean hasKey = actual.getStoreEvents().containsKey(key);
		if (!hasKey) {
			for (String event : actual.getStoreEvents().keySet()) {
				System.out.println("Recorded store event: " + event);
			}
		}
		assertTrue("The store event could not be found. {" + indexName + "} {" + indexType + "} {" + documentId + "}", hasKey);
		return this;
	}

	/**
	 * Verify that the search provider recorded the given create event.
	 * 
	 * @param indexName
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasCreate(String indexName) {
		boolean hasKey = actual.getCreateIndexEvents().contains(indexName);
		if (!hasKey) {
			for (String event : actual.getCreateIndexEvents()) {
				System.out.println("Recorded create event: " + event);
			}
		}
		assertTrue("The create event could not be found. {" + indexName + "}", hasKey);
		return this;
	}

	/**
	 * Verify that the search provider recorded the given delete event.
	 * 
	 * @param indexName
	 * @param indexType
	 * @param documentId
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasDelete(String indexName, String indexType, String documentId) {
		String key = indexName + "-" + indexType + "-" + documentId;
		boolean hasKey = actual.getDeleteEvents().contains(key);
		if (!hasKey) {
			for (String event : actual.getDeleteEvents()) {
				System.out.println("Recorded delete event: " + event);
			}
		}
		assertTrue("The delete event could not be found. {" + indexName + "} - {" + indexType + "} - {" + documentId + "}", hasKey);
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
	 * Verify that the search provider recorded the given update mapping event.
	 * 
	 * @param indexName
	 * @param type
	 * @return
	 */
	public DummySearchProviderAssert hasUpdateMapping(String indexName, String type) {
		boolean hasDrop = actual.getUpdateMappingEvents().containsKey(indexName + "-" + type);
		if (!hasDrop) {
			for (String event : actual.getUpdateMappingEvents().keySet()) {
				System.out.println("Recorded update mapping event: " + event);
			}
		}
		assertTrue("The update mapping event could not be found. {" + indexName + "-" + type + "}", hasDrop);
		return this;
	}

	/**
	 * Assert that the correct count of events was registered.
	 * 
	 * @param storeEvents
	 * @param deleteEvents
	 * @param dropIndexEvents
	 * @param createIndexEvents
	 * @param updateMappingEvents
	 * @return Fluent API
	 */
	public DummySearchProviderAssert hasEvents(int storeEvents, int deleteEvents, int dropIndexEvents, int createIndexEvents,
			int updateMappingEvents) {
		String storeInfo = actual.getStoreEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of store events. Found events: {\n" + storeInfo + "\n}", storeEvents,
				actual.getStoreEvents().size());

		String deleteInfo = actual.getDeleteEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of delete events. Found events: {\n" + deleteInfo + "\n}", deleteEvents,
				actual.getDeleteEvents().size());

		String dropInfo = actual.getDropIndexEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of drop index events. Found events: {\n" + dropInfo + "\n}",
				dropIndexEvents, actual.getDropIndexEvents().size());

		String createInfo = actual.getCreateIndexEvents().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of create index events. Found events: {\n" + createInfo + "\n}",
				createIndexEvents, actual.getCreateIndexEvents().size());

		String updateMappingInfo = actual.getUpdateMappingEvents().keySet().stream().map(Object::toString).reduce((t, u) -> t + "\n" + u).orElse("");
		assertEquals("The search provider did not record the correct amount of update mapping events. Found events: {\n" + updateMappingInfo + "\n}",
				updateMappingEvents, actual.getUpdateMappingEvents().size());

		return this;
	}

	/**
	 * Assert that the node was stored in the index for given languages and DRAFT and PUBLISHED versions
	 * 
	 * @param node
	 * @param test
	 * @param languages
	 * @return Fluent API
	 */
	public DummySearchProviderAssert storedAllContainers(Node node, Project project, Release release, String... languages) {
		for (ContainerType type : Arrays.asList(DRAFT, PUBLISHED)) {
			for (String lang : languages) {
				String projectUuid = project.getUuid();
				String releaseUuid = release.getUuid();
				String schemaVersionUuid = node.getSchemaContainer().getLatestVersion().getUuid();
				assertThat(actual).hasStore(NodeGraphFieldContainer.composeIndexName(projectUuid, releaseUuid, schemaVersionUuid, type),
						NodeGraphFieldContainer.composeIndexType(), NodeGraphFieldContainer.composeDocumentId(node.getUuid(), lang));
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
		assertThat(actual).hasStore(Tag.composeIndexName(tag.getProject().getUuid()), Tag.composeTypeName(), Tag.composeDocumentId(tag.getUuid()));
		return this;
	}

	/**
	 * Assert that the tag family was stored in the index.
	 * 
	 * @param tagfamily
	 * @return Fluent API
	 */
	public DummySearchProviderAssert stored(TagFamily tagfamily) {
		assertThat(actual).hasStore(TagFamily.composeIndexName(tagfamily.getProject().getUuid()), TagFamily.composeTypeName(),
				TagFamily.composeDocumentId(tagfamily.getUuid()));
		return this;
	}

}
