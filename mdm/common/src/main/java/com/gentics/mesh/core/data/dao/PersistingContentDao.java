package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CONTENT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_PUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UNPUBLISHED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_UPDATED;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Objects;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibBinaryField;
import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.data.node.field.HibHtmlField;
import com.gentics.mesh.core.data.node.field.HibNumberField;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.version.VersionInfo;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.DeleteParameters;
import com.gentics.mesh.util.VersionNumber;

public interface PersistingContentDao extends ContentDao {

	/**
	 * Get the node to which this container belongs through the given branch UUID.
	 *
	 * @return
	 */
	HibNode getParentNode(HibNodeFieldContainer container, String branchUuid);

	/**
	 * Repair the inconsistency for the given container.
	 * 
	 * @param container
	 * @return
	 */
	boolean repair(HibNodeFieldContainer container);

	/**
	 * Migrate field container of a node onto the new branch.
	 * 
	 * @param container container to migrate
	 * @param newBranch branch to migrate to
	 * @param node container owning node
	 * @param batch event queue for the notifications
	 * @param container type
	 * @param setInitial is this branch initial for the project?
	 */
	void migrateContainerOntoBranch(HibNodeFieldContainer container, HibBranch newBranch, 
			HibNode node, EventQueueBatch batch, ContainerType containerType, boolean setInitial);

	/**
	 * Get the final type of the micronode entity.
	 * 
	 * @return
	 */
	Class<? extends HibMicronode> getMicronodePersistenceClass();

	/**
	 * Create a container in the persisted storage, according to the root node.
	 * 
	 * @param version mandatory schema version root
	 * @param uuid a UUID to use. If null, a generated UUID will be used.
	 * @return
	 */
	HibNodeFieldContainer createPersisted(HibSchemaVersion version, String uuid);

	/**
	 * Connect fresh container to the node.
	 * 
	 * @param node
	 * @param container
	 * @param branch
	 * @param languageTag
	 * @param handleDraftEdge
	 */
	void connectFieldContainer(HibNode node, HibNodeFieldContainer container, HibBranch branch, String languageTag, boolean handleDraftEdge);

	// Those are stubs. They will be replaced during ContentDao implementation.

	@Deprecated
	HibBooleanField createBoolean(HibNodeFieldContainer container, String name);
	
	@Deprecated
	HibStringField createString(HibNodeFieldContainer container, String name);

	@Deprecated
	HibNumberField createNumber(HibNodeFieldContainer container, String name);
	
	@Deprecated
	HibDateField createDate(HibNodeFieldContainer container, String name);
	
	@Deprecated
	HibHtmlField createHtml(HibNodeFieldContainer container, String name);
	
	@Deprecated
	HibBinaryField createBinary();

	@Override
	default HibNodeFieldContainer createFieldContainer(HibNode node, String languageTag, HibBranch branch, HibUser editor) {
		return createFieldContainer(node, languageTag, branch, editor, null, true);
	}

	@Override
	default HibNodeFieldContainer createFieldContainer(HibNode node, String languageTag, HibBranch branch, HibUser editor, HibNodeFieldContainer original,
		boolean handleDraftEdge) {
		HibNodeFieldContainer previous = null;

		// check whether there is a current draft version
		if (handleDraftEdge) {
			previous = getFieldContainer(node, languageTag, branch, DRAFT);
		}

		// We need create a new container with no reference, if an original is not provided. 
		// So use the latest version available to use.
		HibSchemaVersion version = Objects.isNull(original) 
				? branch.findLatestSchemaVersion(node.getSchemaContainer()) 
				: original.getSchemaContainerVersion() ;

		// Create the new container
		HibNodeFieldContainer newContainer = createPersisted(version, null);

		newContainer.generateBucketId();
		newContainer.setEditor(editor);
		newContainer.setLastEditedTimestamp();
		newContainer.setLanguageTag(languageTag);
		newContainer.setSchemaContainerVersion(version); // TODO mb unneeded

		if (previous != null) {
			// set the next version number
			newContainer.setVersion(previous.getVersion().nextDraft());
			previous.setNextVersion(newContainer);
		} else {
			// set the initial version number
			newContainer.setVersion(new VersionNumber());
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
		newContainer.updateDisplayFieldValue();

		return newContainer;
	}
	
    @Override
	default Result<HibNodeFieldContainer> getDraftFieldContainers(HibNode node) {
		// FIX ME: We should not rely on specific branches.
		return getFieldContainers(node, node.getProject().getLatestBranch(), DRAFT);
	}

    @Override
	default Result<HibNodeFieldContainer> getFieldContainers(HibNode node, HibBranch branch, ContainerType type) {
		return getFieldContainers(node, branch.getUuid(), type);
	}

	@Override
	default void deleteLanguageContainer(HibNode node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
										 boolean failForLastContainer) {
		NodeDao nodeDao = Tx.get().nodeDao();
		
		// 1. Check whether the container has also a published variant. We need to take it offline in those cases
		HibNodeFieldContainer container = getFieldContainer(node, languageTag, branch, PUBLISHED);
		if (container != null) {
			nodeDao.takeOffline(node, ac, bac, branch, languageTag);
		}

		// 2. Load the draft container and remove it from the branch
		container = getFieldContainer(node, languageTag, branch, DRAFT);
		if (container == null) {
			throw error(NOT_FOUND, "node_no_language_found", languageTag);
		}
		container.deleteFromBranch(branch, bac);
		// No need to delete the published variant because if the container was published the take offline call handled it

		// starting with the old draft, delete all GFC that have no next and are not draft (for other branches)
		HibNodeFieldContainer dangling = container;
		while (dangling != null && !dangling.isDraft() && !dangling.hasNextVersion()) {
			HibNodeFieldContainer toDelete = dangling;
			dangling = toDelete.getPreviousVersion();
			toDelete.delete(bac);
		}

		HibNodeFieldContainer initial = getFieldContainer(node, languageTag, branch, INITIAL);
		if (initial != null) {
			// Remove the initial edge
			nodeDao.removeInitialFieldContainerEdge(node, initial, branch.getUuid());

			// starting with the old initial, delete all GFC that have no previous and are not initial (for other branches)
			dangling = initial;
			while (dangling != null && !dangling.isInitial() && !dangling.hasPreviousVersion()) {
				HibNodeFieldContainer toDelete = dangling;
				// since the GFC "toDelete" was only used by this branch, it can not have more than one "next" GFC
				// (multiple "next" would have to belong to different branches, and for every branch, there would have to be
				// an INITIAL, which would have to be either this GFC or a previous)
				dangling = getNextVersions(toDelete).iterator().next();
				toDelete.delete(bac, false);
			}
		}

		// 3. Check whether this was be the last container of the node for this branch
		DeleteParameters parameters = ac.getDeleteParameters();
		if (failForLastContainer) {
			Result<HibNodeFieldContainer> draftContainers = getFieldContainers(node, branch.getUuid(), DRAFT);
			Result<HibNodeFieldContainer> publishContainers = getFieldContainers(node, branch.getUuid(), PUBLISHED);
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
    default HibNodeFieldContainer publish(HibNode node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
		String branchUuid = branch.getUuid();

		// create published version
		HibNodeFieldContainer newVersion = createFieldContainer(node, languageTag, branch, user);
		newVersion.setVersion(newVersion.getVersion().nextPublished());

		Tx.get().nodeDao().setPublished(node, ac, newVersion, branchUuid);
		return newVersion;
    }

	@Override
	default NodeMeshEventModel onDeleted(HibNodeFieldContainer container, String branchUuid, ContainerType type) {
		return createEvent(NODE_CONTENT_DELETED, container, branchUuid, type);
	}

	@Override
	default NodeMeshEventModel onUpdated(HibNodeFieldContainer container, String branchUuid, ContainerType type) {
		return createEvent(NODE_UPDATED, container, branchUuid, type);
	}

	@Override
	default NodeMeshEventModel onCreated(HibNodeFieldContainer container, String branchUuid, ContainerType type) {
		return createEvent(NODE_CONTENT_CREATED, container, branchUuid, type);
	}

	@Override
	default NodeMeshEventModel onTakenOffline(HibNodeFieldContainer container, String branchUuid) {
		return createEvent(NODE_UNPUBLISHED, container, branchUuid, ContainerType.PUBLISHED);
	}

	@Override
	default NodeMeshEventModel onPublish(HibNodeFieldContainer container, String branchUuid) {
		return createEvent(NODE_PUBLISHED, container, branchUuid, ContainerType.PUBLISHED);
	}

	@Override
	default VersionInfo transformToVersionInfo(HibNodeFieldContainer container, InternalActionContext ac) {
		String branchUuid = Tx.get().getBranch(ac).getUuid();
		VersionInfo info = new VersionInfo();
		info.setVersion(container.getVersion().getFullVersion());
		info.setCreated(container.getLastEditedDate());
		HibUser editor = container.getEditor();
		if (editor != null) {
			info.setCreator(editor.transformToReference());
		}
		info.setPublished(container.isPublished(branchUuid));
		info.setDraft(container.isDraft(branchUuid));
		info.setBranchRoot(container.isInitial());
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
	private NodeMeshEventModel createEvent(MeshEvent event, HibNodeFieldContainer container, String branchUuid, ContainerType type) {
		NodeMeshEventModel model = new NodeMeshEventModel();
		model.setEvent(event);
		HibNode node = getParentNode(container, branchUuid);
		String nodeUuid = node.getUuid();
		model.setUuid(nodeUuid);
		model.setBranchUuid(branchUuid);
		model.setLanguageTag(container.getLanguageTag());
		model.setType(type);
		HibSchemaVersion version = container.getSchemaContainerVersion();
		if (version != null) {
			model.setSchema(version.transformToReference());
		}
		HibProject project = node.getProject();
		model.setProject(project.transformToReference());
		return model;
	}
}
