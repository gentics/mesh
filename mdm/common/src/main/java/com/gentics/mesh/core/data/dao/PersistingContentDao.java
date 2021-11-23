package com.gentics.mesh.core.data.dao;

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

	// Those are stubs. They will be replaced during ContentDao implementation.

	/**
	 * Get the final type of the micronode entity.
	 * 
	 * @return
	 */
	Class<? extends HibMicronode> getMicronodePersistenceClass();

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
    default HibNodeFieldContainer getFieldContainer(HibNode node, String languageTag, HibBranch branch, ContainerType type) {
        return Tx.get().nodeDao().getFieldContainer(node, languageTag, branch, type);
    }

    @Override
    default Result<HibNodeFieldContainer> getDraftFieldContainers(HibNode node) {
        return Tx.get().nodeDao().getDraftFieldContainers(node);
    }

    @Override
    default Result<HibNodeFieldContainer> getFieldContainers(HibNode node, String branchUuid, ContainerType type) {
        return Tx.get().nodeDao().getFieldContainers(node, branchUuid, type);
    }

    @Override
    default Result<HibNodeFieldContainer> getFieldContainers(HibNode node, ContainerType type) {
        return Tx.get().nodeDao().getFieldContainers(node, type);
    }

    @Override
    default void deleteLanguageContainer(HibNode node, InternalActionContext ac, HibBranch branch, String languageTag, BulkActionContext bac,
                                         boolean failForLastContainer) {
        Tx.get().nodeDao().deleteLanguageContainer(node, ac, branch, languageTag, bac, failForLastContainer);
    }

    @Override
    default void deleteFromBranch(HibNode node, InternalActionContext ac, HibBranch branch, BulkActionContext bac, boolean ignoreChecks) {
        Tx.get().nodeDao().deleteFromBranch(node, ac, branch, bac, ignoreChecks);
    }

    @Override
    default HibNodeFieldContainer publish(HibNode node, InternalActionContext ac, String languageTag, HibBranch branch, HibUser user) {
        return Tx.get().nodeDao().publish(node, ac, languageTag, branch, user);
    }
}
