package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * Tag specific checks.
 */
public class TagFamilyCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (TagFamily tagFamily : boot.tagFamilyRoot().findAllIt()) {
			checkTagFamily(tagFamily, response);
		}
	}

	private void checkTagFamily(TagFamily tagFamily, ConsistencyCheckResponse response) {
		String uuid = tagFamily.getUuid();

		if (tagFamily.getTagFamilyRoot() == null) {
			response.addInconsistency("The tag family root for tag family could not be located", tagFamily.getUuid(), HIGH);
		}
		if (tagFamily.getCreationTimestamp() == null) {
			response.addInconsistency("The tagFamily creation date is not set", uuid, MEDIUM);
		}
		if (tagFamily.getCreator() == null) {
			response.addInconsistency("The tagFamily creator is not set", uuid, MEDIUM);
		}
		if (tagFamily.getEditor() == null) {
			response.addInconsistency("The tagFamily editor is not set", uuid, MEDIUM);
		}
		if (tagFamily.getLastEditedTimestamp() == null) {
			response.addInconsistency("The tagFamily edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
