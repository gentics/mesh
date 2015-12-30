package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BATCH;
import static com.gentics.mesh.core.data.search.SearchQueueBatch.BATCH_ID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SearchQueue
 */
public class SearchQueueImpl extends MeshVertexImpl implements SearchQueue {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueImpl.class);

	public static void checkIndices(Database database) {
		database.addVertexType(SearchQueueImpl.class);
	}

	@Override
	synchronized public SearchQueueBatch take() throws InterruptedException {
		SearchQueueBatch batch = out(HAS_BATCH).nextOrDefault(SearchQueueBatchImpl.class, null);
		if (batch != null) {
			remove(batch);
		}
		return batch;
	}

	@Override
	synchronized public SearchQueueBatch take(String batchId) {
		SearchQueueBatch batch = out(HAS_BATCH).has(BATCH_ID_PROPERTY_KEY).nextOrDefault(SearchQueueBatchImpl.class, null);
		if (batch != null) {
			remove(batch);
		}
		return batch;
	}

	@Override
	public void add(SearchQueueBatch batch) {
		setLinkOutTo(batch.getImpl(), HAS_BATCH);
	}

	@Override
	public void remove(SearchQueueBatch batch) {
		unlinkOut(batch.getImpl(), HAS_BATCH);
	}

	@Override
	public SearchQueueBatch createBatch(String batchId) {
		SearchQueueBatch batch = getGraph().addFramedVertex(SearchQueueBatchImpl.class);
		batch.setBatchId(batchId);
		batch.setTimestamp(System.currentTimeMillis());
		add(batch);
		return batch;
	}

	@Override
	public long getSize() {
		return out(HAS_BATCH).count();
	}

	@Override
	public void addFullIndex() {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		SearchQueueBatch batch = searchQueue.createBatch(UUIDUtil.randomUUID());
		for (Node node : boot.nodeRoot().findAll()) {
			batch.addEntry(node, CREATE_ACTION);
		}
		for (Project project : boot.projectRoot().findAll()) {
			batch.addEntry(project, CREATE_ACTION);
		}
		for (User user : boot.userRoot().findAll()) {
			batch.addEntry(user, CREATE_ACTION);
		}
		for (Role role : boot.roleRoot().findAll()) {
			batch.addEntry(role, CREATE_ACTION);
		}
		for (Group group : boot.groupRoot().findAll()) {
			batch.addEntry(group, CREATE_ACTION);
		}
		for (Tag tag : boot.tagRoot().findAll()) {
			batch.addEntry(tag, CREATE_ACTION);
		}
		for (TagFamily tagFamily : boot.tagFamilyRoot().findAll()) {
			batch.addEntry(tagFamily, CREATE_ACTION);
		}
		for (SchemaContainer schema : boot.schemaContainerRoot().findAll()) {
			batch.addEntry(schema, CREATE_ACTION);
		}
		// TODO add support for microschemas
		// for (Microschema microschema : boot.microschemaContainerRoot().findAll()) {
		// searchQueue.put(microschema, CREATE_ACTION);
		// }
		if (log.isDebugEnabled()) {
			log.debug("Search Queue size:" + searchQueue.getSize());
		}

	}

	@Override
	public long processAll() throws InterruptedException {
		SearchQueueBatch batch;
		long count = 0;
		while ((batch = take()) != null) {
			batch.process().toBlocking().last();

			if (log.isDebugEnabled()) {
				log.debug("Proccessed batch.");
			}
			count++;
		}
		MeshSpringConfiguration.getInstance().searchProvider().refreshIndex();
		return count;
	}

}
