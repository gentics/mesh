package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Tag specific consistency checks.
 */
public class TagCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Tag> it = db.getVerticesForType(TagImpl.class);
		while (it.hasNext()) {
			checkTag(it.next(), response);
		}
	}

	private void checkTag(Tag tag, ConsistencyCheckResponse response) {
		String uuid = tag.getUuid();

		// checkOut(tag, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(tag, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(tag.getName())) {
			response.addInconsistency("Tag has no name", uuid, HIGH);
		}
		if (tag.getTagFamily() == null) {
			response.addInconsistency("Tag has no tag family linked to it", uuid, HIGH);
		}
		if (tag.getCreationTimestamp() == null) {
			response.addInconsistency("The tag creation date is not set", uuid, MEDIUM);
		}
		if (tag.getLastEditedTimestamp() == null) {
			response.addInconsistency("The tag edit timestamp is not set", uuid, MEDIUM);
		}

	}

}
