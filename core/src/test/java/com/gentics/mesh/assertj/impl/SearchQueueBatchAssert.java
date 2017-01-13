package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.HandleContext;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.UpdateBatchEntry;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.graphdb.spi.Database;

public class SearchQueueBatchAssert extends AbstractAssert<SearchQueueBatchAssert, SearchQueueBatch> {

	public SearchQueueBatchAssert(SearchQueueBatch actual) {
		super(actual, SearchQueueBatchAssert.class);
	}

	public SearchQueueBatchAssert hasEntries(int count) {
		isNotNull();
		assertEquals("The search queue did not contain the expected amount of entries.", count, actual.getEntries().size());
		return this;
	}

	/**
	 * Validate the list of expected entries by checking whether they were removed or not and also check whether the provided search queue batch contains the
	 * expected entries.
	 * 
	 * @param expectedEntries
	 * @return Fluent API
	 */
	public SearchQueueBatchAssert containsEntries(Map<String, ElementEntry> expectedEntries) {
		long nExpectedBatchEntries = 0;

		// Check each expected elements
		for (String key : expectedEntries.keySet()) {
			ElementEntry entry = expectedEntries.get(key);

			// 1. Check for deletion from graph
			if (DELETE_ACTION.equals(entry.getAction()) && entry.getType() == null) {
				assertFalse("The element {" + key + "} vertex for uuid: {" + entry.getUuid() + "}",
						Database.getThreadLocalGraph().v().has("uuid", entry.getUuid()).hasNext());
			}
			// 2. Check batch entries
			if (entry.getAction() != null) {
				if (!entry.getLanguages().isEmpty()) {
					// Check each language individually since the document id is constructed (uuid+lang)
					for (String language : entry.getLanguages()) {

						SearchQueueEntry foundMatch = null;
						for (SearchQueueEntry currentEntry : actual.getEntries()) {
							HandleContext context = currentEntry.getContext();
							if (entry.getProjectUuid() != null && !entry.getProjectUuid().equals(context.getProjectUuid())) {
								// Project uuid does not match up - check next
								continue;
							}
							if (entry.getReleaseUuid() != null && !entry.getReleaseUuid().equals(context.getReleaseUuid())) {
								// Release uuid does not match up - check next
								continue;
							}
							if (entry.getType() != null && !entry.getType().equals(context.getContainerType())) {
								// type does not match up - check next
								continue;
							}

							// Check whether the languages of the entry does not match up.
							if (context.getLanguageTag() != null && !language.equals(context.getLanguageTag())) {
								// Language does not match up - check next
								continue;
							}
							if (currentEntry instanceof UpdateBatchEntry) {
								UpdateBatchEntry be = (UpdateBatchEntry) currentEntry;
								if (!be.getElementUuid().equals(entry.getUuid())) {
									// Element uuid does not match up - check next
									continue;
								}
							}
							if (context.getLanguageTag() == null) {
								System.out.println("HALT");
							}
							foundMatch = currentEntry;
							break;
						}

						if (foundMatch != null) {
							for (SearchQueueEntry be : actual.getEntries()) {
								System.out.println(be.toString());
							}
						}
						assertThat(foundMatch).as("Entry for {" + key + "}/{" + entry.getUuid() + "} - language {" + language + "} Entries:\n")
								.isNotNull();
						assertEquals("The created batch entry for {" + key + "} language {" + language + "} did not use the expected action",
								entry.getAction(), foundMatch.getElementAction());
						nExpectedBatchEntries++;
					}
				} else {
					//					Optional<? extends SearchQueueEntry> batchEntry = batch.findEntryByUuid(entry.getUuid());
					//					assertThat(batchEntry).as("Entry for {" + key + "}/{" + entry.getUuid() + "}").isPresent();
					//					SearchQueueEntry batchEntryValue = batchEntry.get();
					//					assertEquals("The created batch entry for {" + key + "} did not use the expected action", entry.getAction(),
					//							batchEntryValue.getElementAction());
					//					nExpectedBatchEntries++;
				}
			}
		}
		return this;
	}

}
