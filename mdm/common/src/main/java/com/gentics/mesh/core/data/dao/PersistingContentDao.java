package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.common.ContainerType.forVersion;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.rest.error.Errors.nodeConflict;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static java.util.Objects.nonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.NodeMigrationActionContextImpl;
import com.gentics.mesh.core.data.DeletableField;
import com.gentics.mesh.core.data.Field;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.NodeFieldContainerEdge;
import com.gentics.mesh.core.data.branch.Branch;
import com.gentics.mesh.core.data.diff.FieldChangeTypes;
import com.gentics.mesh.core.data.diff.FieldContainerChange;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BinaryField;
import com.gentics.mesh.core.data.node.field.DisplayField;
import com.gentics.mesh.core.data.node.field.StringField;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.s3binary.S3BinaryField;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.error.NameConflictException;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.job.warning.ConflictWarning;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.FieldMapImpl;
import com.gentics.mesh.core.rest.node.field.FieldModel;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.core.result.TraversalResult;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.path.Path;
import com.gentics.mesh.path.PathSegment;
import com.gentics.mesh.path.impl.PathImpl;
import com.gentics.mesh.path.impl.PathSegmentImpl;
import com.gentics.mesh.util.StreamUtil;
import com.gentics.mesh.util.UniquenessUtil;
import com.gentics.mesh.util.VersionNumber;
import com.google.common.base.Equivalence;
import com.google.common.collect.MapDifference;
import com.google.common.collect.Maps;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface PersistingContentDao extends ContentDao {

	Logger log = LoggerFactory.getLogger(PersistingContentDao.class);

	/**
	 * Get the node to which this container belongs through the given branch UUID.
	 *
	 * @return
	 */
	Node getParentNode(NodeFieldContainer container, String branchUuid);

	/**
	 * Delete the distinct field explicitly.
	 * 
	 * @param field
	 */
	void deleteField(DeletableField field);

	/**
	 * Get a field container version, that is not attached to a storage, 
	 * and thus can be reused after being removed from storage into the transient state.
	 * Override this method, if manipulating a weak reference to the field, to obtain an actual entity or its DTO.
	 * 
	 * @param field
	 * @return
	 */
	default Field detachField(Field field) {
		return field;
	}

	/**
	 * Migrate field container of a node onto the new branch.
	 * 
	 * @param container container to migrate
	 * @param newBranch branch to migrate to
	 * @param node container owning node
	 * @param batch event queue for the notifications
	 * @param container type
	 * @param setInitial @Override
	is this branch initial for the project?
	 */
	default void migrateContainerOntoBranch(NodeFieldContainer container, Branch newBranch, Node node, EventQueueBatch batch, ContainerType containerType, boolean setInitial) {
		if (setInitial) {
			createContainerEdge(node, container, newBranch, container.getLanguageTag(), INITIAL);
		}
		NodeFieldContainerEdge edge = createContainerEdge(node, container, newBranch, container.getLanguageTag(), containerType);
		String value = getSegmentFieldValue(container);
		Node parent = CommonTx.get().nodeDao().getParentNode(node, newBranch.getUuid());
		if (value != null) {
			edge.setSegmentInfo(parent, value);
		} else {
			edge.setSegmentInfo(null);
		}
		edge.setUrlFieldInfo(getUrlFieldValues(container).collect(Collectors.toSet()));
		batch.add(onUpdated(container, newBranch.getUuid(), containerType));
	}

	/**
	 * 
	 * @param branchUuid
	 * @param type
	 * @param segment
	 * @return
	 */
	default PathSegment getSegment(Node node, String branchUuid, ContainerType type, String segment) {
		// Check the different language versions
		for (NodeFieldContainer container : getFieldContainers(node, branchUuid, type)) {
			SchemaModel schema = getSchemaContainerVersion(container).getSchema();
			String segmentFieldName = schema.getSegmentField();
			// First check whether a string field exists for the given name
			StringField field = container.getString(segmentFieldName);
			if (field != null) {
				String fieldValue = field.getString();
				if (segment.equals(fieldValue)) {
					return new PathSegmentImpl(container, field, container.getLanguageTag(), segment);
				}
			}

			// No luck yet - lets check whether a binary field matches the
			// segmentField
			BinaryField binaryField = container.getBinary(segmentFieldName);
			if (binaryField == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + node.getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
						+ "}");
				}
			} else {
				String binaryFilename = binaryField.getFileName();
				if (segment.equals(binaryFilename)) {
					return new PathSegmentImpl(container, binaryField, container.getLanguageTag(), segment);
				}
			}
			// No luck yet - lets check whether a S3 binary field matches the segmentField
			S3BinaryField s3Binary = container.getS3Binary(segmentFieldName);
			if (s3Binary == null) {
				if (log.isDebugEnabled()) {
					log.debug("The node {" + node.getUuid() + "} did not contain a string or a binary field for segment field name {" + segmentFieldName
							+ "}");
				}
			} else {
				String s3binaryFilename = s3Binary.getBinary().getFileName();
				if (segment.equals(s3binaryFilename)) {
					return new PathSegmentImpl(container, s3Binary, container.getLanguageTag(), segment);
				}
			}
		}
		return null;
	}

	/**
	 * Find all micronodes. Used by the indexing engine and tests.
	 * 
	 * @return
	 */
	Stream<? extends Micronode> findAllMicronodes();

	/**
	 * Create a container in the persisted storage, according to the root node.
	 *
	 * @param nodeUUID the node of this graph field container
	 * @param version mandatory schema version root
	 * @param uuid a UUID to use. If null, a generated UUID will be used.
	 * @param languageTag optional language tag
	 * @param versionNumber mandatory versionNumber
	 * @param editor optional user
	 * @return
	 */
	NodeFieldContainer createPersisted(String nodeUUID, SchemaVersion version, String uuid, String languageTag, VersionNumber versionNumber, User editor);

	/**
	 * Connect fresh container to the node.
	 * 
	 * @param node
	 * @param newContainer
	 * @param branch
	 * @param languageTag
	 * @param handleDraftEdge
	 */
	default void connectFieldContainer(Node node, NodeFieldContainer newContainer, Branch branch, String languageTag, boolean handleDraftEdge) {
		String branchUuid = branch.getUuid();

		if (handleDraftEdge) {
			NodeFieldContainerEdge oldDraftEdge = getEdge(node, languageTag, branchUuid, DRAFT);
			String segmentInfo = null;
			if (oldDraftEdge != null) {
				segmentInfo = oldDraftEdge.getSegmentInfo();
			}

			// remove existing draft edge
			if (oldDraftEdge != null) {
				removeEdge(oldDraftEdge);
			}
			// create a new draft edge
			NodeFieldContainerEdge newDraftEdge = createContainerEdge(node, newContainer, branchUuid, languageTag, DRAFT);
			if (!StringUtils.isEmpty(segmentInfo)) {
				newDraftEdge.setSegmentInfo(segmentInfo);
			}

			updateWebrootPathInfo(newContainer, branchUuid, "node_conflicting_segmentfield_update", false);
		}

		// if there is no initial edge, create one
		if (getEdge(node, languageTag, branchUuid, INITIAL) == null) {
			createContainerEdge(node, newContainer, branchUuid, languageTag, INITIAL);
		}
	}

	/**
	 * Create an edge connection between the container and its node, based on the given edge properties.
	 *
	 * @param node
	 * @param container
	 * @param branch
	 * @param languageTag
	 * @param initial
	 * @return
	 */
	NodeFieldContainerEdge createContainerEdge(Node node, NodeFieldContainer container, Branch branch,
			String languageTag, ContainerType initial);

	/**
	 * Remove the connection edge.
	 *
	 * @param edge
	 */
	void removeEdge(NodeFieldContainerEdge edge);

	/**
	 * Find the node edge with the given parameters: language, branch, type
	 *
	 * @param node
	 * @param languageTag
	 * @param branchUuid
	 * @param type
	 * @return
	 */
	NodeFieldContainerEdge getEdge(Node node, String languageTag, String branchUuid, ContainerType type);

	/**
	 * Get the content container the edge is pointing to
	 *
	 * @param edge
	 * @return
	 */
	NodeFieldContainer getFieldContainerOfEdge(NodeFieldContainerEdge edge);

	default NodeFieldContainerEdge createContainerEdge(Node node, NodeFieldContainer container, String branchUUID, String languageTag, ContainerType initial) {
		Branch branch = Tx.get().branchDao().findByUuid(node.getProject(), branchUUID);
		return createContainerEdge(node, container, branch, languageTag, initial);
	}

	@Override
	default NodeFieldContainer createFieldContainer(Node node, String languageTag, Branch branch, User editor) {
		return createFieldContainer(node, languageTag, branch, editor, null, true);
	}

	@Override
	default NodeFieldContainer createFirstFieldContainerForNode(Node node, String languageTag, Branch branch, User editor) {
		SchemaVersion version = branch.findLatestSchemaVersion(node.getSchemaContainer());
		NodeFieldContainer newContainer = createPersisted(node.getUuid(), version, null, languageTag, new VersionNumber(), editor);
		connectFieldContainer(node, newContainer, branch, languageTag, true);

		return newContainer;
	}

	@Override
	default NodeFieldContainer createFieldContainer(Node node, String languageTag, Branch branch, User editor, NodeFieldContainer original,
													   boolean handleDraftEdge) {
		// We need create a new container with no reference, if an original is not provided.
		// So use the latest version available to use.
		SchemaVersion version = Objects.isNull(original)
				? branch.findLatestSchemaVersion(node.getSchemaContainer())
				: getSchemaContainerVersion(original) ;
		return createFieldContainer(version, node, languageTag, branch, editor, original, handleDraftEdge);
	}

	private NodeFieldContainer createFieldContainer(SchemaVersion version, Node node, String languageTag, Branch branch, User editor, NodeFieldContainer original,
		boolean handleDraftEdge) {
		NodeFieldContainer previous = null;

		// check whether there is a current draft version
		if (handleDraftEdge) {
			previous = getFieldContainer(node, languageTag, branch, DRAFT);
		}

		// Create the new container
		VersionNumber versionNumber = previous != null ? previous.getVersion().nextDraft() : new VersionNumber();
		NodeFieldContainer newContainer = createPersisted(node.getUuid(), version, null, languageTag, versionNumber, editor);

		if (previous != null) {
			// set the next version number
			previous.setNextVersion(newContainer);
		}

		// clone the original or the previous container
		if (original != null) {
			newContainer.clone(original);
		} else if (previous != null) {
			newContainer.clone(previous);
		}

		connectFieldContainer(node, newContainer, branch, languageTag, handleDraftEdge);

		// We need to update the display field property since we created a new
		// node graph field container.
		updateDisplayFieldValue(newContainer);

		return newContainer;
	}

	@Override
	default NodeFieldContainer createEmptyFieldContainer(SchemaVersion version, Node node, User editor, String languageTag, Branch branch) {
		NodeFieldContainer previous = getFieldContainer(node, languageTag, branch, DRAFT);

		VersionNumber versionNumber = previous != null ? previous.getVersion().nextDraft() : new VersionNumber();
		NodeFieldContainer newContainer = createPersisted(node.getUuid(), version, null, languageTag, versionNumber, editor);

		if (previous != null) {
			previous.setNextVersion(newContainer);
		}

		connectFieldContainer(node, newContainer, branch, languageTag, true);

		updateDisplayFieldValue(newContainer);

		return newContainer;
	}
	
    @Override
	default Result<NodeFieldContainer> getDraftFieldContainers(Node node) {
		// FIX ME: We should not rely on specific branches.
		return getFieldContainers(node, CommonTx.get().branchDao().getLatestBranch(node.getProject()), DRAFT);
	}

    @Override
	default Result<NodeFieldContainer> getFieldContainers(Node node, Branch branch, ContainerType type) {
		return getFieldContainers(node, branch.getUuid(), type);
	}

	@Override
	default void deleteLanguageContainer(Node node, InternalActionContext ac, Branch branch, String languageTag, BulkActionContext bac,
										 boolean failForLastContainer) {
		NodeDao nodeDao = Tx.get().nodeDao();
		
		// 1. Check whether the container has also a published variant. We need to take it offline in those cases
		NodeFieldContainer container = getFieldContainer(node, languageTag, branch, PUBLISHED);
		if (container != null) {
			nodeDao.takeOffline(node, ac, bac, branch, languageTag);
		}

		// 2. Load the draft container and remove it from the branch
		container = getFieldContainer(node, languageTag, branch, DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", languageTag);
		}
		deleteFromBranch(container, branch, bac);
		// No need to delete the published variant because if the container was published the take offline call handled it

		// starting with the old draft, delete all GFC that have no next and are not draft (for other branches)
		NodeFieldContainer dangling = container;
		while (dangling != null && !isDraft(dangling) && !dangling.hasNextVersion()) {
			NodeFieldContainer toDelete = dangling;
			dangling = toDelete.getPreviousVersion();
			delete(toDelete, bac);
		}

		NodeFieldContainer initial = getFieldContainer(node, languageTag, branch, INITIAL);
		if (initial != null) {
			// Remove the initial edge
			nodeDao.removeInitialFieldContainerEdge(node, initial, branch.getUuid());

			// starting with the old initial, delete all GFC that have no previous and are not initial (for other branches)
			dangling = initial;
			while (dangling != null && !isInitial(dangling) && !dangling.hasPreviousVersion()) {
				NodeFieldContainer toDelete = dangling;
				// since the GFC "toDelete" was only used by this branch, it can not have more than one "next" GFC
				// (multiple "next" would have to belong to different branches, and for every branch, there would have to be
				// an INITIAL, which would have to be either this GFC or a previous)
				Iterator<NodeFieldContainer> danglingIterator = getNextVersions(toDelete).iterator();
				dangling = danglingIterator.hasNext() ? danglingIterator.next() : null;
				delete(toDelete, bac, false);
			}
		}

		// 3. Check whether this was be the last container of the node for this branch
		DeleteParameters parameters = ac.getDeleteParameters();
		if (failForLastContainer) {
			Result<NodeFieldContainer> draftContainers = getFieldContainers(node, branch.getUuid(), DRAFT);
			Result<NodeFieldContainer> publishContainers = getFieldContainers(node, branch.getUuid(), PUBLISHED);
			boolean wasLastContainer = !draftContainers.iterator().hasNext() && !publishContainers.iterator().hasNext();

			if (!parameters.isRecursive() && wasLastContainer) {
				throw error(BAD_REQUEST, "node_error_delete_failed_last_container_for_branch");
			}

			// Also delete the node and children
			if (parameters.isRecursive() && wasLastContainer) {
				nodeDao.deleteFromBranch(node, ac, branch, bac, false);
			}
		}
	}

    @Override
    default NodeFieldContainer publish(Node node, InternalActionContext ac, String languageTag, Branch branch, User user) {
		String branchUuid = branch.getUuid();

		// create published version
		NodeFieldContainer newVersion = createFieldContainer(node, languageTag, branch, user);
		Tx.get().contentDao().setVersion(newVersion, newVersion.getVersion().nextPublished());

		Tx.get().nodeDao().setPublished(node, ac, newVersion, branchUuid);
		return newVersion;
    }

	@Override
	default NodeMeshEventModel onDeleted(NodeFieldContainer container, String branchUuid, ContainerType type) {
		return createEvent(NODE_CONTENT_DELETED, container, branchUuid, type);
	}

	@Override
	default NodeMeshEventModel onUpdated(NodeFieldContainer container, String branchUuid, ContainerType type) {
		return createEvent(NODE_UPDATED, container, branchUuid, type);
	}

	@Override
	default NodeMeshEventModel onCreated(NodeFieldContainer container, String branchUuid, ContainerType type) {
		return createEvent(NODE_CONTENT_CREATED, container, branchUuid, type);
	}

	@Override
	default NodeMeshEventModel onTakenOffline(NodeFieldContainer container, String branchUuid) {
		return createEvent(NODE_UNPUBLISHED, container, branchUuid, ContainerType.PUBLISHED);
	}

	@Override
	default NodeMeshEventModel onPublish(NodeFieldContainer container, String branchUuid) {
		return createEvent(NODE_PUBLISHED, container, branchUuid, ContainerType.PUBLISHED);
	}

	@Override
	default VersionInfo transformToVersionInfo(NodeFieldContainer container, InternalActionContext ac) {
		String branchUuid = Tx.get().getBranch(ac).getUuid();
		VersionInfo info = new VersionInfo();
		info.setVersion(container.getVersion().getFullVersion());
		info.setCreated(container.getLastEditedDate());
		User editor = container.getEditor();
		if (editor != null) {
			info.setCreator(editor.transformToReference());
		}
		info.setPublished(isPublished(container, branchUuid));
		info.setDraft(isDraft(container, branchUuid));
		info.setBranchRoot(isInitial(container));
		return info;
	}

	/**
	 * Create a new node event.
	 * 
	 * @param event
	 *            Type of the event
	 * @param container 
	 * @param branchUuid
	 *            Branch Uuid if known
	 * @param type
	 *            Type of the node content if known
	 * @return Created model
	 */
	private NodeMeshEventModel createEvent(MeshEvent event, NodeFieldContainer container, String branchUuid, ContainerType type) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		Node node = getParentNode(container, branchUuid);
		String nodeUuid = node.getUuid();
		model.setUuid(nodeUuid);
		model.setBranchUuid(branchUuid);
		model.setLanguageTag(container.getLanguageTag());
		model.setType(type);
		SchemaVersion version = getSchemaContainerVersion(container);
		if (version != null) {
			model.setSchema(version.transformToReference());
		}
		Project project = node.getProject();
		model.setProject(project.transformToReference());
		return model;
	}

	@Override
	default void updateWebrootPathInfo(NodeFieldContainer content, InternalActionContext ac, String branchUuid, String conflictI18n, boolean checkForConflicts) {
		Set<String> urlFieldValues = getUrlFieldValues(content).collect(Collectors.toSet());
		Iterator<? extends NodeFieldContainerEdge> it = getContainerEdges(content, DRAFT, branchUuid);
		if (it.hasNext()) {
			NodeFieldContainerEdge draftEdge = it.next();
			updateWebrootPathInfo(content, ac, draftEdge, branchUuid, conflictI18n, DRAFT);
			updateWebrootUrlFieldsInfo(content, draftEdge, branchUuid, urlFieldValues, DRAFT, checkForConflicts);
		}
		it = getContainerEdges(content, PUBLISHED, branchUuid);
		if (it.hasNext()) {
			NodeFieldContainerEdge publishEdge = it.next();
			updateWebrootPathInfo(content, ac, publishEdge, branchUuid, conflictI18n, PUBLISHED);
			updateWebrootUrlFieldsInfo(content, publishEdge, branchUuid, urlFieldValues, PUBLISHED, checkForConflicts);
		}
	}

	/**
	 * Determine the display field value by checking the schema and the referenced field and store it as a property.
	 */
	@Override
	default void updateDisplayFieldValue(NodeFieldContainer container) {
		// TODO use schema storage instead
		SchemaModel schema = container.getSchemaContainerVersion().getSchema();
		String displayFieldName = schema.getDisplayField();
		FieldSchema fieldSchema = schema.getField(displayFieldName);
		// Only update the display field value if the field can be located
		if (fieldSchema != null) {
			Field field = container.getField(fieldSchema);
			if (field != null && field instanceof DisplayField) {
				DisplayField displayField = (DisplayField) field;
				setDisplayFieldValue(container, displayField.getDisplayName());
				return;
			}
		}
		// Otherwise reset the value to null
		setDisplayFieldValue(container, null);
	}

	/**
	 * Set the container's display field value.
	 * 
	 * @param container
	 * @param value
	 */
	void setDisplayFieldValue(NodeFieldContainer container, String value);

	/**
	 * Update the webroot path info (checking for uniqueness before)
	 *
	 * @param content
	 * @param ac
	 * @param edge
	 * @param branchUuid
	 *            branch Uuid
	 * @param conflictI18n
	 *            i18n for the message in case of conflict
	 * @param type
	 *            edge type
	 */
	private void updateWebrootPathInfo(NodeFieldContainer content, InternalActionContext ac, NodeFieldContainerEdge edge, String branchUuid, String conflictI18n,
										 ContainerType type) {
		final int MAX_NUMBER = 255;
		Node node = getNode(content);
		String segmentFieldName = getSchemaContainerVersion(content).getSchema().getSegmentField();
		String languageTag = getLanguageTag(content);

		// Handle node migration conflicts automagically
		if (ac instanceof NodeMigrationActionContextImpl) {
			NodeMigrationActionContextImpl nmac = (NodeMigrationActionContextImpl) ac;
			ConflictWarning info = null;
			for (int i = 0; i < MAX_NUMBER; i++) {
				try {
					if (updateWebrootPathInfo(content, node, edge, branchUuid, segmentFieldName, conflictI18n, type)) {
						break;
					}
				} catch (NameConflictException e) {
					// Only throw the exception if we tried multiple renames
					if (i >= MAX_NUMBER - 1) {
						throw e;
					} else {
						// Generate some information about the found conflict
						info = new ConflictWarning();
						info.setNodeUuid(node.getUuid());
						info.setBranchUuid(branchUuid);
						info.setType(type.name());
						info.setLanguageTag(languageTag);
						info.setFieldName(segmentFieldName);
						postfixPathSegment(node, branchUuid, type, languageTag);
					}
				}
			}
			// We encountered a conflict which was resolved. Lets add that info to the context
			if (info != null) {
				nmac.addConflictInfo(info);
			}
		} else {
			updateWebrootPathInfo(content, node, edge, branchUuid, segmentFieldName, conflictI18n, type);
		}
	}

	/**
	 * Postfix the path segment for the container that matches the given parameters. This operation is not needed for basenodes (since segment must be / for
	 * those anyway).
	 *
	 * @param branchUuid
	 * @param type
	 * @param languageTag
	 */
	default void postfixPathSegment(Node node, String branchUuid, ContainerType type, String languageTag) {
		NodeDao nodeDao = Tx.get().nodeDao();
		// Check whether this node is the base node.
		if (nodeDao.getParentNode(node, branchUuid) == null) {
			return;
		}

		// Find the first matching container and fallback to other listed languages
		NodeFieldContainer container = getFieldContainer(node, languageTag, branchUuid, type);
		if (container != null) {
			postfixSegmentFieldValue(container);
		}
	}

	/**
	 * update the webroot path, returning whether the operation succeeded
	 *
	 * @param content
	 * @param node
	 * @param edge
	 * @param branchUuid
	 * @param segmentFieldName
	 * @param conflictI18n
	 * @param type
	 * @return
	 */
	private boolean updateWebrootPathInfo(NodeFieldContainer content, Node node, NodeFieldContainerEdge edge, String branchUuid, String segmentFieldName,
										  String conflictI18n,
										  ContainerType type) {
		NodeDao nodeDao = Tx.get().nodeDao();

		// Determine the webroot path of the container parent node
		String segment = getPathSegment(node, branchUuid, type, getLanguageTag(content));

		// The webroot uniqueness will be checked by validating that the string [segmentValue-branchUuid-parentNodeUuid] is only listed once within the given
		// specific index for (drafts or published nodes). Segment field should also be set, otherwise the segment info is treated as outdated and is subject to reset.
		if (segment != null && StringUtils.isNotBlank(segmentFieldName)) {
			Node parentNode = nodeDao.getParentNode(node, branchUuid);
			String segmentInfo = composeSegmentInfo(parentNode, segment);

			String currentSegmentInfo = edge.getSegmentInfo();
			if (StringUtils.equals(segmentInfo, currentSegmentInfo) || (StringUtils.isEmpty(segmentInfo) && StringUtils.isEmpty(currentSegmentInfo))) {
				return true;
			}

			// check for uniqueness of webroot path
			NodeFieldContainerEdge conflictingEdge = getConflictingEdgeOfWebrootPath(content, segmentInfo, branchUuid, type, edge);
			if (conflictingEdge != null) {
				Node conflictingNode = conflictingEdge.getNode();
				NodeFieldContainer conflictingContainer = conflictingEdge.getNodeContainer();
				if (log.isDebugEnabled()) {
					log.debug("Found conflicting container with uuid {" + conflictingContainer.getUuid() + "} of node {" + conflictingNode.getUuid()
							+ "}");
				}
				throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguageTag(),
						conflictI18n, segmentFieldName, segment);
			} else {
				edge.setSegmentInfo(segmentInfo);
				return true;
			}
		} else {
			edge.setSegmentInfo(null);
			return true;
		}
	}

	@Override
	default String composeSegmentInfo(Node parentNode, String segment) {
		return parentNode == null ? "" : parentNode.getUuid() + segment;
	}

	/**
	 * Update the webroot url field index and also assert that the new values would not cause a conflict with the existing data.
	 *
	 * @param content
	 * @param edge
	 * @param branchUuid
	 * @param urlFieldValues
	 * @param type
	 * @param checkForConflicts true when the check for conflicting values must be done, false if not
	 */
	private void updateWebrootUrlFieldsInfo(NodeFieldContainer content, NodeFieldContainerEdge edge, String branchUuid, Set<String> urlFieldValues, ContainerType type, boolean checkForConflicts) {
		if (urlFieldValues != null && !urlFieldValues.isEmpty()) {
			if (checkForConflicts) {
				NodeFieldContainerEdge conflictingEdge = getConflictingEdgeOfWebrootField(content, edge, urlFieldValues, branchUuid, type);
				if (conflictingEdge != null) {
					NodeFieldContainer conflictingContainer = conflictingEdge.getNodeContainer();
					Node conflictingNode = conflictingEdge.getNode();
					if (log.isDebugEnabled()) {
						log.debug(
								"Found conflicting container with uuid {" + conflictingContainer.getUuid() + "} of node {" + conflictingNode.getUuid());
					}
					// We know that the found container already occupies the index with one of the given paths. Lets compare both sets of paths in order to
					// determine
					// which path caused the conflict.
					Set<String> fromConflictingContainer = getUrlFieldValues(conflictingContainer).collect(Collectors.toSet());
					@SuppressWarnings("unchecked")
					Collection<String> conflictingValues = CollectionUtils.intersection(fromConflictingContainer, urlFieldValues);
					String paths = String.join(",", conflictingValues);

					throw nodeConflict(conflictingNode.getUuid(), conflictingContainer.getDisplayFieldValue(), conflictingContainer.getLanguageTag(),
							"node_conflicting_urlfield_update", paths, conflictingContainer.getNode().getUuid(),
							conflictingContainer.getLanguageTag(), fromConflictingContainer.stream().collect(Collectors.joining(", ")));
				}
			}
			edge.setUrlFieldInfo(urlFieldValues);
		} else {
			edge.setUrlFieldInfo(null);
		}
	}

	@Override
	default Stream<String> getUrlFieldValues(NodeFieldContainer content) {
		SchemaVersionModel schema = getSchemaContainerVersion(content).getSchema();

		List<String> urlFields = schema.getUrlFields();
		if (urlFields == null) {
			return Stream.empty();
		}
		return urlFields.stream()
				.filter(urlField -> {
							FieldSchema field = schema.getField(urlField);
							return field instanceof StringFieldSchema ||
									(field instanceof ListFieldSchema && "string".equals(((ListFieldSchema) field).getListType()));
						}
				)
				.flatMap(urlField -> {
					FieldSchema fieldSchema = schema.getField(urlField);
					Field field = content.getField(fieldSchema);
					if (field instanceof StringField) {
						StringField stringField = (StringField) field;
						String value = stringField.getString();
						if (StringUtils.isBlank(value)) {
							return Stream.empty();
						} else {
							return Stream.of(value);
						}
					}
					if (field instanceof StringFieldList) {
						StringFieldList stringListField = (StringFieldList) field;
						return stringListField.getList().stream()
								.flatMap(listField -> Optional.ofNullable(listField)
										.map(StringField::getString)
										.filter(StringUtils::isNotBlank)
										.stream());
					}

					return Stream.empty();
				});
	}

	@Override
	default String getPathSegment(Node node, String branchUuid, ContainerType type, boolean anyLanguage, String... languageTag) {
		// Check whether this node is the base node.
		if (node.getParentNode(branchUuid) == null) {
			return "";
		}

		// Find the first matching container and fallback to other listed languages
		NodeFieldContainer container = null;
		for (String tag : languageTag) {
			if ((container = getFieldContainer(node, tag, branchUuid, type)) != null) {
				break;
			}
		}

		if (container == null && anyLanguage) {
			Result<? extends NodeFieldContainerEdge> traversal = getFieldEdges(node, branchUuid, type);

			if (traversal.hasNext()) {
				container = traversal.next().getNodeContainer();
			}
		}

		if (container != null) {
			return getSegmentFieldValue(container);
		}
		return null;

	}

	default String getSegmentFieldValue(NodeFieldContainer content) {
		SchemaVersionModel schema = getSchemaContainerVersion(content).getSchema();
		String segmentFieldKey = schema.getSegmentField();
		// 1. The container may reference a schema which has no segment field set thus no path segment can be determined
		if (segmentFieldKey == null) {
			return null;
		}

		FieldTypes segmentFieldType = FieldTypes.valueByName(schema.getField(segmentFieldKey).getType());
		if (segmentFieldType == null) {
			return getStringField(content, segmentFieldKey);
		}

		switch (segmentFieldType) {
			case BINARY:
				String binaryFileName = content.getBinaryFileName(segmentFieldKey);
				if (nonNull(binaryFileName)) {
					return binaryFileName;
				}
				break;
			case S3BINARY:
				String s3BinaryFileName = content.getS3BinaryFileName(segmentFieldKey);
				if (nonNull(s3BinaryFileName)) {
					return s3BinaryFileName;
				}
				break;
			default:
				return getStringField(content, segmentFieldKey);
		}

		return null;
	}

	private String getStringField(NodeFieldContainer content, String segmentFieldKey) {
		StringField stringField = content.getString(segmentFieldKey);
		if (stringField != null) {
			return stringField.getString();
		}
		return null;
	}

	@Override
	default void postfixSegmentFieldValue(NodeFieldContainer content) {
		String segmentFieldKey = getSchemaContainerVersion(content).getSchema().getSegmentField();
		// 1. The container may reference a schema which has no segment field set thus no path segment can be determined
		if (segmentFieldKey == null) {
			return;
		}

		// 2. Try to load the path segment using the string field
		StringField stringField = content.getString(segmentFieldKey);
		if (stringField != null) {
			String oldValue = stringField.getString();
			if (oldValue != null) {
				stringField.setString(UniquenessUtil.suggestNewName(oldValue));
			}
		}

		// 3. Try to load the path segment using the binary field since the string field could not be found
		if (stringField == null) {
			BinaryField binaryField = content.getBinary(segmentFieldKey);
			if (binaryField != null) {
				binaryField.postfixFileName();
			}
		}
	}

	@Override
	default List<FieldContainerChange> compareTo(NodeFieldContainer content, NodeFieldContainer container) {
		List<FieldContainerChange> changes = new ArrayList<>();

		SchemaModel schemaA = getSchemaContainerVersion(content).getSchema();
		Map<String, FieldSchema> fieldMapA = schemaA.getFieldsAsMap();
		SchemaModel schemaB = getSchemaContainerVersion(container).getSchema();
		Map<String, FieldSchema> fieldMapB = schemaB.getFieldsAsMap();
		// Generate a structural diff first. This way it is easy to determine
		// which fields have been added or removed.
		MapDifference<String, FieldSchema> diff = Maps.difference(fieldMapA, fieldMapB, new Equivalence<FieldSchema>() {

			@Override
			protected boolean doEquivalent(FieldSchema a, FieldSchema b) {
				return a.getName().equals(b.getName());
			}

			@Override
			protected int doHash(FieldSchema t) {
				// TODO Auto-generated method stub
				return 0;
			}

		});

		// Handle fields which exist only in A - They have been removed in B
		for (FieldSchema field : diff.entriesOnlyOnLeft().values()) {
			changes.add(new FieldContainerChange(field.getName(), FieldChangeTypes.REMOVED));
		}

		// Handle fields which don't exist in A - They have been added in B
		for (FieldSchema field : diff.entriesOnlyOnRight().values()) {
			changes.add(new FieldContainerChange(field.getName(), FieldChangeTypes.ADDED));
		}

		// Handle fields which are common in both schemas
		for (String fieldName : diff.entriesInCommon().keySet()) {
			FieldSchema fieldSchemaA = fieldMapA.get(fieldName);
			FieldSchema fieldSchemaB = fieldMapB.get(fieldName);
			// Check whether the field type is different in between both schemas
			if (fieldSchemaA.getType().equals(fieldSchemaB.getType())) {
				// Check content
				Field fieldA = content.getField(fieldSchemaA);
				Field fieldB = container.getField(fieldSchemaB);
				// Handle null cases. The field may not have been created yet.
				if (fieldA != null && fieldB == null) {
					// Field only exists in A
					changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
				} else if (fieldA == null && fieldB != null) {
					// Field only exists in B
					changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
				} else if (fieldA != null && fieldB != null) {
					changes.addAll(fieldA.compareTo(fieldB));
				} else {
					// Both fields are equal if those fields are both null
				}
			} else {
				// The field type has changed
				changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
			}

		}
		return changes;
	}

	@Override
	default List<FieldContainerChange> compareTo(NodeFieldContainer content, FieldMap fieldMap) {
		List<FieldContainerChange> changes = new ArrayList<>();

		SchemaModel schemaA = getSchemaContainerVersion(content).getSchema();
		Map<String, FieldSchema> fieldSchemaMap = schemaA.getFieldsAsMap();

		// Handle all fields
		for (String fieldName : fieldSchemaMap.keySet()) {
			FieldSchema fieldSchema = fieldSchemaMap.get(fieldName);
			// Check content
			Field fieldA = content.getField(fieldSchema);
			FieldModel fieldB = fieldMap.getField(fieldName, fieldSchema);
			// Handle null cases. The field may not have been created yet.
			if (fieldA != null && fieldB == null && fieldMap.hasField(fieldName)) {
				// Field only exists in A
				changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
			} else if (fieldA == null && fieldB != null) {
				// Field only exists in B
				changes.add(new FieldContainerChange(fieldName, FieldChangeTypes.UPDATED));
			} else if (fieldA != null && fieldB != null) {
				// Field exists in A and B and the fields are not equal to each
				// other.
				changes.addAll(fieldA.compareTo(fieldB));
			} else {
				// Both fields are equal if those fields are both null
			}

		}
		return changes;
	}

	@Override
	default Path getPath(NodeFieldContainer content, InternalActionContext ac) {
		Path nodePath = new PathImpl();
		nodePath.addSegment(new PathSegmentImpl(content, null, getLanguageTag(content), null));
		return nodePath;
	}

	@Override
	default void deleteFromBranch(NodeFieldContainer content, Branch branch, BulkActionContext bac) {
		String branchUuid = branch.getUuid();

		bac.batch().add(onDeleted(content, branchUuid, DRAFT));
		if (isPublished(content, branchUuid)) {
			bac.batch().add(onDeleted(content, branchUuid, PUBLISHED));
		}

		Node node = getNode(content);
		NodeFieldContainerEdge draft = getEdge(node, content.getLanguageTag(), branch.getUuid(), DRAFT);
		if (draft != null) {
			removeEdge(draft);	
		}

		NodeFieldContainerEdge published = getEdge(node, content.getLanguageTag(), branch.getUuid(), PUBLISHED);
		if (published != null) {
			removeEdge(published);	
		}
	}

	@Override
	default void purge(NodeFieldContainer content, BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Purging container {" + content.getUuid() + "} for version {" + content.getVersion() + "}");
		}
		// Link the previous to the next to isolate the old container
		NodeFieldContainer beforePrev = content.getPreviousVersion();
		if (beforePrev != null) {
			for (NodeFieldContainer afterPrev : content.getNextVersions()) {
				beforePrev.setNextVersion(afterPrev);
			}
		}
		delete(content, bac, false);
	}

	@Override
	default boolean isAutoPurgeEnabled(NodeFieldContainer content) {
		SchemaVersion schema = getSchemaContainerVersion(content);
		return schema.isAutoPurgeEnabled();
	}

	@Override
	default String getETag(NodeFieldContainer content, InternalActionContext ac) {
		return content.getETag(ac);
	}

	@Override
	default Result<NodeFieldContainer> versions(NodeFieldContainer content) {
		return new TraversalResult<>(StreamUtil.untilNull(() -> content, NodeFieldContainer::getPreviousVersion));
	}

	@Override
	default NodeFieldListItem toListItem(Node node, InternalActionContext ac, String[] languageTags) {
		CommonTx tx = CommonTx.get();
		// Create the rest field and populate the fields
		NodeFieldListItemImpl listItem = new NodeFieldListItemImpl(node.getUuid());
		String branchUuid = tx.getBranch(ac, node.getProject()).getUuid();
		ContainerType type = forVersion(new VersioningParametersImpl(ac).getVersion());
		if (ac.getNodeParameters().getResolveLinks() != LinkType.OFF) {
			listItem.setUrl(tx.data().mesh().webRootLinkReplacer().resolve(ac, branchUuid, type, node, ac.getNodeParameters().getResolveLinks(),
				languageTags));
		}
		return listItem;
	}

	@Override
	default FieldMap getFieldMap(NodeFieldContainer fieldContainer, InternalActionContext ac, SchemaModel schema, int level, List<String> containerLanguageTags) {
		com.gentics.mesh.core.rest.node.FieldMap fields = new FieldMapImpl();
		for (FieldSchema fieldEntry : schema.getFields()) {
			// boolean expandField =
			// fieldsToExpand.contains(fieldEntry.getName()) ||
			// ac.getExpandAllFlag();
			FieldModel restField = fieldContainer.getRestField(ac, fieldEntry.getName(), fieldEntry, containerLanguageTags, level);
			if (fieldEntry.isRequired() && restField == null) {
				// TODO i18n
				// throw error(BAD_REQUEST, "The field {" +
				// fieldEntry.getName()
				// + "} is a required field but it could not be found in the
				// node. Please add the field using an update call or change
				// the field schema and
				// remove the required flag.");
				fields.put(fieldEntry.getName(), null);
			}
			if (restField == null) {
				if (log.isDebugEnabled()) {
					log.debug("Field for key {" + fieldEntry.getName() + "} could not be found. Ignoring the field.");
				}
			} else {
				fields.put(fieldEntry.getName(), restField);
			}

		}
		return fields;
	}

	/**
	 * Check whether this field container edge has the given type in the given branch.
	 *
	 * @param type
	 * @param branchUuid
	 * @return true if it matches the type, false if not
	 */
	default boolean isType(NodeFieldContainerEdge edge, ContainerType type, String branchUuid) {
		return edge.getType().equals(type) && edge.getBranchUuid().equals(branchUuid);
	}		
}

