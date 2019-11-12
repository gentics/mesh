package com.gentics.mesh.core.data.node.impl;

import java.security.InvalidParameterException;
import java.util.Objects;

public class BranchParentEntry {
	private final String branchUuid;
	private final String parentUuid;

	private BranchParentEntry(String branchUuid, String parentUuid) {
		this.branchUuid = branchUuid;
		this.parentUuid = parentUuid;
	}

	public static BranchParentEntry branchParentEntry(String branchUuid, String parentUuid) {
		Objects.requireNonNull(branchUuid);
		Objects.requireNonNull(parentUuid);
		return new BranchParentEntry(branchUuid, parentUuid);
	}

	public static BranchParentEntry fromString(String branchParentEntry) {
		Objects.requireNonNull(branchParentEntry);
		String[] split = branchParentEntry.split(":");
		if (split.length != 2) {
			throw new InvalidParameterException(String.format("Expected format <branchUuid>:<parentNodeUuid>, got {%s} instead.", branchParentEntry));
		}
		return new BranchParentEntry(split[0], split[1]);
	}

	public String getBranchUuid() {
		return branchUuid;
	}

	public String getParentUuid() {
		return parentUuid;
	}

	@Override
	public String toString() {
		return branchUuid + ":" + parentUuid;
	}
}
