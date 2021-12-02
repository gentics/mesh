package com.gentics.mesh.core.data.dao;

import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.INITIAL;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

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
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.DeleteParameters;

public interface PersistingContentDao extends ContentDao {

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

	// Those are stubs. They will be replaced during ContentDao implementation.

	@Deprecated
	HibNodeFieldContainer createContainer();
	
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
}
