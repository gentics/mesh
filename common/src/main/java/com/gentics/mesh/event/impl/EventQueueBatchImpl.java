package com.gentics.mesh.event.impl;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.EventCauseAction;
import com.gentics.mesh.core.rest.event.EventCauseInfo;
import com.gentics.mesh.core.rest.event.EventCauseInfoImpl;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @see EventQueueBatch
 */
public class EventQueueBatchImpl implements EventQueueBatch {

	private static final Logger log = LoggerFactory.getLogger(EventQueueBatchImpl.class);

	private String batchId;

	private List<MeshEventModel> bulkEntries = new ArrayList<>();

	private EventCauseInfo cause;

	public EventQueueBatchImpl() {

	}

	@Override
	public List<MeshEventModel> getEntries() {
		return bulkEntries;
	}

	// @Override
	// public EventQueueBatch createIndex(String indexName, Class<?> elementClass) {
	// CreateIndexEntry entry = new CreateIndexEntryImpl(registry.getForClass(elementClass), indexName);
	// addEntry(entry);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch createNodeIndex(String projectUuid, String branchUuid, String versionUuid,
	// ContainerType type, Schema schema) {
	// String indexName = NodeGraphFieldContainer.composeIndexName(projectUuid, branchUuid, versionUuid, type);
	// CreateIndexEntry entry = new CreateIndexEntryImpl(nodeContainerIndexHandler, indexName);
	// entry.setSchema(schema);
	// // entry.getContext().setSchemaContainerVersionUuid(versionUuid);
	// addEntry(entry);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch dropIndex(String indexName) {
	// DropIndexEntry entry = new DropIndexEntryImpl(commonHandler, indexName);
	// addEntry(entry);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch store(Node node, String branchUuid, ContainerType type, boolean addRelatedElements) {
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setContainerType(type);
	// context.setBranchUuid(branchUuid);
	// context.setProjectUuid(node.getProject().getUuid());
	// store((IndexableElement) node, context, addRelatedElements);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch move(NodeGraphFieldContainer oldContainer, NodeGraphFieldContainer newContainer,
	// String branchUuid, ContainerType type) {
	// MoveEntryContext context = new MoveEntryContextImpl();
	// context.setContainerType(type);
	// context.setBranchUuid(branchUuid);
	// context.setOldContainer(oldContainer);
	// context.setNewContainer(newContainer);
	// MoveDocumentEntry entry = new MoveDocumentEntryImpl(nodeContainerIndexHandler, context);
	// addEntry(entry);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch store(NodeGraphFieldContainer container, String branchUuid, ContainerType type,
	// boolean addRelatedElements) {
	// Node node = container.getParentNode();
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setContainerType(type);
	// context.setBranchUuid(branchUuid);
	// context.setLanguageTag(container.getLanguageTag());
	// context.setSchemaContainerVersionUuid(container.getSchemaContainerVersion().getUuid());
	// context.setProjectUuid(node.getProject().getUuid());
	// store((IndexableElement) node, context, addRelatedElements);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(Tag tag, boolean addRelatedEntries) {
	// // We need to add the project uuid to the context because the index handler for
	// // tags will not be able to
	// // determine the project uuid once the tag has been removed from the graph.
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setProjectUuid(tag.getProject().getUuid());
	// delete((IndexableElement) tag, context, addRelatedEntries);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(TagFamily tagFymily, boolean addRelatedEntries) {
	// // We need to add the project uuid to the context because the index handler for
	// // tagfamilies will not be able to
	// // determine the project uuid once the tagfamily has been removed from the
	// // graph.
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setProjectUuid(tagFymily.getProject().getUuid());
	// delete((IndexableElement) tagFymily, context, addRelatedEntries);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(NodeGraphFieldContainer container, String branchUuid, ContainerType type,
	// boolean addRelatedEntries) {
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// context.setContainerType(type);
	// context.setProjectUuid(container.getParentNode().getProject().getUuid());
	// context.setBranchUuid(branchUuid);
	// context.setSchemaContainerVersionUuid(container.getSchemaContainerVersion().getUuid());
	// context.setLanguageTag(container.getLanguageTag());
	// delete((IndexableElement) container.getParentNode(), context, addRelatedEntries);
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch store(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
	// UpdateDocumentEntryImpl entry = new UpdateDocumentEntryImpl(registry.getForClass(element), element, context,
	// STORE_ACTION);
	// addEntry(entry);
	//
	// if (addRelatedEntries) {
	// // We need to store (e.g: Update related entries)
	// element.handleRelatedEntries((relatedElement, relatedContext) -> {
	// store(relatedElement, relatedContext, false);
	// });
	// }
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch delete(IndexableElement element, GenericEntryContext context, boolean addRelatedEntries) {
	// UpdateDocumentEntry entry = new UpdateDocumentEntryImpl(registry.getForClass(element), element, context,
	// DELETE_ACTION);
	// addEntry(entry);
	//
	// if (addRelatedEntries) {
	// // We need to store (e.g: Update related entries)
	// element.handleRelatedEntries((relatedElement, relatedContext) -> {
	// this.store(relatedElement, relatedContext, false);
	// });
	// }
	// return this;
	// }
	//
	// @Override
	// public EventQueueBatch updatePermissions(IndexableElement element) {
	// GenericEntryContextImpl context = new GenericEntryContextImpl();
	// Project project = element.getProject();
	// if (project != null) {
	// context.setProjectUuid(project.getUuid());
	// }
	// UpdateDocumentEntry entry = new UpdateDocumentEntryImpl(registry.getForClass(element), element, context,
	// SearchQueueEntryAction.UPDATE_ROLE_PERM_ACTION);
	// addEntry(entry);
	// return this;
	// }
	//
	// @Override
	// public BulkEventQueueEntry<?> addEntry(BulkEventQueueEntry<?> entry) {
	// bulkEntries.add(entry);
	// return entry;
	// }
	//
	// @Override
	// public SeperateSearchQueueEntry<?> addEntry(SeperateSearchQueueEntry<?> entry) {
	// seperateEntries.add(entry);
	// return entry;
	// }
	//
	// @Override
	// public List<? extends SearchQueueEntry> getEntries() {
	// List<SearchQueueEntry<? extends EntryContext>> entries = Stream
	// .concat(bulkEntries.stream(), seperateEntries.stream()).collect(Collectors.toList());
	//
	// if (log.isDebugEnabled()) {
	// for (SearchQueueEntry entry : entries) {
	// log.debug("Loaded entry {" + entry.toString() + "} for batch {" + getBatchId() + "}");
	// }
	// }
	//
	// return entries;
	// }
	//

	//
	// @Override
	// public void printDebug() {
	// for (SearchQueueEntry entry : getEntries()) {
	// log.debug("Entry {" + entry.toString() + "} in batch {" + getBatchId() + "}");
	// }
	// }
	//
	// @Override
	// public void clear() {
	// bulkEntries.clear();
	// seperateEntries.clear();
	// }
	//
	// @Override
	// public int size() {
	// return bulkEntries.size() + seperateEntries.size();
	// }
	//
	// @Override
	// public void addAll(EventQueueBatch otherBatch) {
	// if (otherBatch instanceof EventQueueBatchImpl) {
	// EventQueueBatchImpl batch = (EventQueueBatchImpl) otherBatch;
	// bulkEntries.addAll(batch.bulkEntries);
	// seperateEntries.addAll(batch.seperateEntries);
	// } else {
	// throw new RuntimeException("Cannot mix SearchQueueBatch instances");
	// }
	// }
	//
	//

	@Override
	public void setCause(ElementType type, String uuid, EventCauseAction action) {
		this.cause = new EventCauseInfoImpl(type, uuid, action);
	}

	@Override
	public void setCause(EventCauseInfo cause) {
		this.cause = cause;
	}

	@Override
	public EventCauseInfo getCause() {
		return cause;
	}

	@Override
	public String getBatchId() {
		return batchId;
	}

	@Override
	public void dispatch() {
		EventBus eventbus = Mesh.vertx().eventBus();
		// TODO buffer event dispatching?
		getEntries().forEach(entry -> {
			entry.setCause(getCause());
			MeshEvent event = entry.getEvent();
			if (log.isDebugEnabled()) {
				log.debug("Created event sent {}", event);
			}
			String json = JsonUtil.toJson(entry);
			if (log.isTraceEnabled()) {
				log.trace("Dispatching event '{}' with payload:\n{}", event, json);
			}
			eventbus.publish(event.getAddress(), new JsonObject(json));
		});
		getEntries().clear();
	}

}
