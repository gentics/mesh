package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.graphdb.spi.Database;

/**
 * Tag specific consistency checks.
 */
public class TagCheck implements ConsistencyCheck {

	@Override
	public void invoke(Database db, ConsistencyCheckResponse response) {
		Iterator<? extends Tag> it = db.getVerticesForType(TagImpl.class);
		while (it.hasNext()) {
			checkTag(it.next(), response);
		}
	}

	private void checkTag(Tag tag, ConsistencyCheckResponse response) {
		String uuid = tag.getUuid();

		if (isEmpty(tag.getName())) {
			response.addInconsistency("Tag has no name", uuid, HIGH);
		}
		if (tag.getTagFamily() == null) {
			response.addInconsistency("Tag has no tag family linked to it", uuid, HIGH);
		}
		if (tag.getCreationTimestamp() == null) {
			response.addInconsistency("The tag creation date is not set", uuid, MEDIUM);
		}
		if (tag.getCreator() == null) {
			response.addInconsistency("The tag creator is not set", uuid, MEDIUM);
		}
		if (tag.getEditor() == null) {
			response.addInconsistency("The tag editor is not set", uuid, MEDIUM);
		}
		if (tag.getLastEditedTimestamp() == null) {
			response.addInconsistency("The tag edit timestamp is not set", uuid, MEDIUM);
		}

	}

}
