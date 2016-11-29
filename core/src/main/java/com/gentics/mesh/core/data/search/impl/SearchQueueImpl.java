package com.gentics.mesh.core.data.search.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BATCH;
import static com.gentics.mesh.core.data.search.SearchQueueBatch.BATCH_ID_PROPERTY_KEY;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see SearchQueue
 */
public class SearchQueueImpl extends MeshVertexImpl implements SearchQueue {

	private static final Logger log = LoggerFactory.getLogger(SearchQueueImpl.class);

	public static void init(Database database) {
		database.addVertexType(SearchQueueImpl.class, MeshVertexImpl.class);
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
		setUniqueLinkOutTo(batch.getImpl(), HAS_BATCH);
	}

	@Override
	public void remove(SearchQueueBatch batch) {
		unlinkOut(batch.getImpl(), HAS_BATCH);
	}

	@Override
	public SearchQueueBatch createBatch() {
		SearchQueueBatch batch = getGraph().addFramedVertex(SearchQueueBatchImpl.class);
		batch.setBatchId(UUIDUtil.randomUUID());
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
		BootstrapInitializer boot = MeshInternal.get().boot();
		SearchQueue queue = MeshInternal.get().boot().meshRoot().getSearchQueue();
		SearchQueueBatch batch = queue.createBatch();
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
			log.debug("Search Queue size:" + getSize());
		}

	}

	@Override
	public long processAll() throws InterruptedException {
		SearchQueueBatch batch;
		long count = 0;
		while ((batch = take()) != null) {
			if (batch.getEntries().size() > 0) {
				batch.processSync(30, TimeUnit.MINUTES);
			}
			if (log.isDebugEnabled()) {
				log.debug("Proccessed batch.");
			}
			count++;
		}
		if (count > 0) {
			MeshInternal.get().searchProvider().refreshIndex();
		}
		return count;
	}

	@Override
	public void clear() {
		List<? extends SearchQueueBatchImpl> batches = out(HAS_BATCH).toListExplicit(SearchQueueBatchImpl.class);
		for (SearchQueueBatchImpl batch : batches) {
			batch.delete();
		}
	}

}
