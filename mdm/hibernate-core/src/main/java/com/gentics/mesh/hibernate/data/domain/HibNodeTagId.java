package com.gentics.mesh.hibernate.data.domain;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * Composite ID for Node-Tag edge. 
 * 
 * @see HibNodeTag
 * @author plyhun
 *
 */
@Embeddable
public class HibNodeTagId implements Serializable {

	private static final long serialVersionUID = -4615070706688160294L;

	@Column(nullable = false)
	private UUID nodeUUID;

	@Column(nullable = false)
	private UUID tagUUID;

	@Column(nullable = false)
	private UUID branchUUID;

	public HibNodeTagId() {
	}

	public HibNodeTagId(HibNodeImpl node, HibTagImpl tag, HibBranchImpl branch) {
		this.nodeUUID = node.getDbUuid();
		this.tagUUID = tag.getDbUuid();
		this.branchUUID = branch.getDbUuid();
	}

	public UUID getNodeUUID() {
		return nodeUUID;
	}

	public void setNodeUUID(UUID nodeUUID) {
		this.nodeUUID = nodeUUID;
	}

	public UUID getTagUUID() {
		return tagUUID;
	}

	public void setTagUUID(UUID tagUUID) {
		this.tagUUID = tagUUID;
	}

	public UUID getBranchUUID() {
		return branchUUID;
	}

	public void setBranchUUID(UUID branchUUID) {
		this.branchUUID = branchUUID;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		HibNodeTagId that = (HibNodeTagId) o;
		return Objects.equals(nodeUUID, that.nodeUUID) && Objects.equals(tagUUID, that.tagUUID)
				&& Objects.equals(branchUUID, that.branchUUID);
	}

	@Override
	public int hashCode() {
		return Objects.hash(nodeUUID, tagUUID, branchUUID);
	}
}
