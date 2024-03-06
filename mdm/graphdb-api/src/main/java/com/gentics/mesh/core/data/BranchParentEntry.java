package com.gentics.mesh.core.data;

import java.security.InvalidParameterException;
import java.util.Objects;

import com.gentics.mesh.core.data.relationship.GraphRelationships;

/**
 * <p>Represents the format used in the set with the key defined in {@link GraphRelationships#BRANCH_PARENTS_KEY_PROPERTY}</p>
 * <p>Use {@link #branchParentEntry(String, String)} to create an entry from uuids.</p>
 * <p>Use {@link #fromString(String)} to parse an entry to the uuids.</p>
 */
public class BranchParentEntry {
	private final String branchUuid;
	private final String parentUuid;

	private BranchParentEntry(String branchUuid, String parentUuid) {
		this.branchUuid = branchUuid;
		this.parentUuid = parentUuid;
	}

	/**
	 * Create a new entry.
	 * @param branchUuid
	 * @param parentUuid
	 * @return
	 */
	public static BranchParentEntry branchParentEntry(String branchUuid, String parentUuid) {
		Objects.requireNonNull(branchUuid);
		Objects.requireNonNull(parentUuid);
		return new BranchParentEntry(branchUuid, parentUuid);
	}

	/**
	 * Parse an entry that was previously created with {@link #encode()}.
	 * @param branchParentEntry
	 * @return
	 */
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

	/**
	 * Encodes the branch uuid and parent uuid to a string, which can later be parsed with {@link #fromString(String)}.
	 * @return
	 */
	public String encode() {
		return branchUuid + ":" + parentUuid;
	}

	@Override
	public String toString() {
		return encode();
	}
}
