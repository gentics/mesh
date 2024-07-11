package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Embeddable ID of the entity node_branch_parent. The ID is composed of the
 * child UUID and the branch UUID
 */
@Embeddable
public class HibBranchNodeParentId implements Serializable {

	private static final long serialVersionUID = 1L;

	@Column
	private UUID childUuid;

	@Column
	private UUID nodeParentUuid;

	@Column
	private UUID branchParentUuid;

	public HibBranchNodeParentId() {
	}

	public HibBranchNodeParentId(UUID childUuid, UUID nodeParentUuid, UUID branchParentUUID) {
		this.childUuid = childUuid;
		this.nodeParentUuid = nodeParentUuid;
		this.branchParentUuid = branchParentUUID;
	}

	public UUID getChildUUID() {
		return childUuid;
	}

	public void setChildUUID(UUID childrenParentUUID) {
		this.childUuid = childrenParentUUID;
	}

	public UUID getNodeParentUuid() {
		return nodeParentUuid;
	}

	public void setNodeParentUuid(UUID nodeParentUuid) {
		this.nodeParentUuid = nodeParentUuid;
	}

	public UUID getBranchParentUUID() {
		return branchParentUuid;
	}

	public void setBranchParentUUID(UUID branchParentUUID) {
		this.branchParentUuid = branchParentUUID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HibBranchNodeParentId that = (HibBranchNodeParentId) o;
		return Objects.equals(childUuid, that.childUuid) && Objects.equals(nodeParentUuid, that.nodeParentUuid) && Objects.equals(branchParentUuid, that.branchParentUuid);
	}

	@Override
	public int hashCode() {
		return Objects.hash(childUuid, nodeParentUuid, branchParentUuid);
	}
}
