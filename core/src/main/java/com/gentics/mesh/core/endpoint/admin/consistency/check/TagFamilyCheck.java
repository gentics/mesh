package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;
import com.gentics.mesh.graphdb.spi.Database;
import com.syncleus.ferma.tx.Tx;

/**
 * Tag specific checks.
 */
public class TagFamilyCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "tagFamilies";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, TagFamilyImpl.class, (tagFamily, result) -> {
			checkTagFamily(tagFamily, result);
		}, attemptRepair, tx);
	}

	private void checkTagFamily(TagFamily tagFamily, ConsistencyCheckResult result) {
		String uuid = tagFamily.getUuid();

		// checkOut(tagFamily, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(tagFamily, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (tagFamily.getTagFamilyRoot() == null) {
			result.addInconsistency("The tag family root for tag family could not be located", tagFamily.getUuid(), HIGH);
		}
		if (tagFamily.getCreationTimestamp() == null) {
			result.addInconsistency("The tagFamily creation date is not set", uuid, MEDIUM);
		}
		if (tagFamily.getLastEditedTimestamp() == null) {
			result.addInconsistency("The tagFamily edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
