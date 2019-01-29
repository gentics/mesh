package com.gentics.mesh.assertj.impl;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.DELETE_ACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Map;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.search.SearchQueueEntry;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.context.EntryContext;
import com.gentics.mesh.core.data.search.context.GenericEntryContext;
import com.gentics.mesh.core.node.ElementEntry;
import com.gentics.mesh.event.EventQueueBatch;
import com.syncleus.ferma.tx.Tx;

public class SearchQueueBatchAssert extends AbstractAssert<SearchQueueBatchAssert, EventQueueBatch> {

	public SearchQueueBatchAssert(EventQueueBatch actual) {
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

		// Check each expected elements
		for (String key : expectedEntries.keySet()) {
			ElementEntry entry = expectedEntries.get(key);

			// 1. Check for deletion from graph
			if (DELETE_ACTION.equals(entry.getAction()) && entry.getType() == null) {
				assertFalse("The element {" + key + "} vertex for uuid: {" + entry.getUuid() + "}",
						Tx.getActive().getGraph().v().has("uuid", entry.getUuid()).hasNext());
			}
			// 2. Check batch entries
			if (entry.getAction() != null) {
				if (!entry.getLanguages().isEmpty()) {
					// Check each language individually since the document id is constructed (uuid+lang)
					for (String language : entry.getLanguages()) {

						SearchQueueEntry foundMatch = null;
						for (SearchQueueEntry currentEntry : actual.getEntries()) {
							EntryContext context = currentEntry.getContext();
							if (context instanceof GenericEntryContext) {
								GenericEntryContext genericContext = (GenericEntryContext)context;
								if (entry.getProjectUuid() != null && !entry.getProjectUuid().equals(genericContext.getProjectUuid())) {
									// Project uuid does not match up - check next
									continue;
								}
								if (entry.getBranchUuid() != null && !entry.getBranchUuid().equals(genericContext.getBranchUuid())) {
									// Branch uuid does not match up - check next
									continue;
								}
								if (entry.getType() != null && !entry.getType().equals(genericContext.getContainerType())) {
									// type does not match up - check next
									continue;
								}

								// Check whether the languages of the entry does not match up.
								if (genericContext.getLanguageTag() != null && !language.equals(genericContext.getLanguageTag())) {
									// Language does not match up - check next
									continue;
								}
								if (currentEntry instanceof UpdateDocumentEntry) {
									UpdateDocumentEntry be = (UpdateDocumentEntry) currentEntry;
									if (!be.getElementUuid().equals(entry.getUuid())) {
										// Element uuid does not match up - check next
										continue;
									}
								}
								if (genericContext.getLanguageTag() == null) {
									System.out.println("HALT");
								}
								foundMatch = currentEntry;
							}
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
					}
				}
			}
		}
		return this;
	}

}
