package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Tag specific consistency checks.
 */
public class TagCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "tags";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, TagImpl.class, (tag, result) -> {
			checkTag(tag, result);
		}, attemptRepair, tx);
	}

	private void checkTag(Tag tag, ConsistencyCheckResult result) {
		String uuid = tag.getUuid();

		// checkOut(tag, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(tag, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(tag.getName())) {
			result.addInconsistency("Tag has no name", uuid, HIGH);
		}
		if (tag.getTagFamily() == null) {
			result.addInconsistency("Tag has no tag family linked to it", uuid, HIGH);
		}
		if (tag.getCreationTimestamp() == null) {
			result.addInconsistency("The tag creation date is not set", uuid, MEDIUM);
		}
		if (tag.getLastEditedTimestamp() == null) {
			result.addInconsistency("The tag edit timestamp is not set", uuid, MEDIUM);
		}
		if (tag.getBucketId() == null) {
			result.addInconsistency("The tag bucket id is not set", uuid, MEDIUM);
		}


	}

}
