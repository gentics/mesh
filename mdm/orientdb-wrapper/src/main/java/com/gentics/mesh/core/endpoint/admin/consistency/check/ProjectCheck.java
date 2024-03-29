package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.admin.consistency.AbstractConsistencyCheck;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheckResult;

/**
 * Project specific checks.
 */
public class ProjectCheck extends AbstractConsistencyCheck {

	@Override
	public String getName() {
		return "projects";
	}

	@Override
	public ConsistencyCheckResult invoke(Database db, Tx tx, boolean attemptRepair) {
		return processForType(db, ProjectImpl.class, (project, result) -> {
			checkProject(project, result);
		}, attemptRepair, tx);
	}

	private void checkProject(Project project, ConsistencyCheckResult result) {
		String uuid = project.getUuid();

		checkIn(project, HAS_PROJECT, ProjectRootImpl.class, result, HIGH);

		checkOut(project, HAS_BRANCH_ROOT, BranchRootImpl.class, result, HIGH);
		checkOut(project, HAS_NODE_ROOT, NodeRootImpl.class, result, HIGH);
		checkOut(project, HAS_TAGFAMILY_ROOT, TagFamilyRootImpl.class, result, HIGH);
		checkOut(project, HAS_ROOT_NODE, NodeImpl.class, result, HIGH);
		checkOut(project, HAS_SCHEMA_ROOT, ProjectSchemaContainerRootImpl.class, result, HIGH);
		checkOut(project, HAS_MICROSCHEMA_ROOT, ProjectMicroschemaContainerRootImpl.class, result, HIGH);

		// checkOut(project, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(project, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(project.getName())) {
			result.addInconsistency("Project name is empty or not set", uuid, HIGH);
		}
		if (project.getCreationTimestamp() == null) {
			result.addInconsistency("The project creation date is not set", uuid, MEDIUM);
		}
		if (project.getLastEditedTimestamp() == null) {
			result.addInconsistency("The project edit timestamp is not set", uuid, MEDIUM);
		}
		if (project.getBucketId() == null) {
			result.addInconsistency("The project bucket id is not set", uuid, MEDIUM);
		}
	}

}
