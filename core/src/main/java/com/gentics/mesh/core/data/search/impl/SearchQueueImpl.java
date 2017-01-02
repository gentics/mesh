package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SearchQueue
 */
public class SearchQueueImpl implements SearchQueue {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueImpl.class);

	@Override
	public SearchQueueBatch createBatch() {
		return new SearchQueueBatchImpl();
	}

	@Override
	public SearchQueueBatch addFullIndex() {
		BootstrapInitializer boot = MeshInternal.get().boot();
		SearchQueueBatch batch = createBatch();
		for (Node node : boot.nodeRoot().findAll()) {
			node.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (Project project : boot.projectRoot().findAll()) {
			project.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (User user : boot.userRoot().findAll()) {
			user.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (Role role : boot.roleRoot().findAll()) {
			role.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (Group group : boot.groupRoot().findAll()) {
			group.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (Tag tag : boot.tagRoot().findAll()) {
			tag.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (TagFamily tagFamily : boot.tagFamilyRoot().findAll()) {
			tagFamily.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		for (SchemaContainer schema : boot.schemaContainerRoot().findAll()) {
			schema.addIndexBatchEntry(batch, STORE_ACTION, false);
		}
		// TODO add support for microschemas
		// for (Microschema microschema : boot.microschemaContainerRoot().findAll()) {
		// searchQueue.put(microschema, CREATE_ACTION);
		// }
		if (log.isDebugEnabled()) {
			log.debug("Search Queue Batch size:" + batch.getEntries().size());
		}
		return batch;
	}

}
