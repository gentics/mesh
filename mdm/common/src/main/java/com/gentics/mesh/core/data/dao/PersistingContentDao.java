package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.result.Result;

public interface PersistingContentDao extends ContentDao {

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
