package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * Release specific consistency checks.
 */
public class ReleaseCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (Project project : boot.projectRoot().findAll()) {
			for (Release release : project.getReleaseRoot().findAll()) {
				checkRelease(release, response);
			}
		}
	}

	private void checkRelease(Release release, ConsistencyCheckResponse response) {
		String uuid = release.getUuid();

		if (isEmpty(release.getName())) {
			response.addInconsistency("Release name is empty or not set", uuid, HIGH);
		}
		if (release.getCreationTimestamp() == null) {
			response.addInconsistency("The release creation date is not set", uuid, MEDIUM);
		}
		if (release.getCreator() == null) {
			response.addInconsistency("The release creator is not set", uuid, MEDIUM);
		}
		if (release.getEditor() == null) {
			response.addInconsistency("The release editor is not set", uuid, MEDIUM);
		}
		if (release.getLastEditedTimestamp() == null) {
			response.addInconsistency("The release edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
