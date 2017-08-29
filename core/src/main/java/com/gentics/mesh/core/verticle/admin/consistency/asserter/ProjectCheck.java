package com.gentics.mesh.core.verticle.admin.consistency.asserter;

import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.core.verticle.admin.consistency.ConsistencyCheck;

/**
 * Project specific checks.
 */
public class ProjectCheck implements ConsistencyCheck {

	@Override
	public void invoke(BootstrapInitializer boot, ConsistencyCheckResponse response) {
		for (Project project : boot.projectRoot().findAll()) {
			checkProject(project, response);
		}
	}

	private void checkProject(Project project, ConsistencyCheckResponse response) {
		String uuid = project.getUuid();

		if (project.getLatestRelease() == null) {
			response.addInconsistency("Project has no latest release", uuid, HIGH);
		}
		if (project.getInitialRelease() == null) {
			response.addInconsistency("Project has no initial release", uuid, HIGH);
		}
		if (isEmpty(project.getName())) {
			response.addInconsistency("Project name is empty or not set", uuid, HIGH);
		}
		if (project.getBaseNode() == null) {
			response.addInconsistency("The project must have a base node", uuid, HIGH);
		}
		if (project.getCreationTimestamp() == null) {
			response.addInconsistency("The project creation date is not set", uuid, MEDIUM);
		}
		if (project.getCreator() == null) {
			response.addInconsistency("The project creator is not set", uuid, MEDIUM);
		}
		if (project.getEditor() == null) {
			response.addInconsistency("The project editor is not set", uuid, MEDIUM);
		}
		if (project.getLastEditedTimestamp() == null) {
			response.addInconsistency("The project edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
