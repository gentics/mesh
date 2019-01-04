package com.gentics.mesh.core.endpoint.admin.consistency.check;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_MICROSCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_BRANCH_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROOT_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_SCHEMA_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.HIGH;
import static com.gentics.mesh.core.rest.admin.consistency.InconsistencySeverity.MEDIUM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Iterator;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.root.impl.NodeRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectMicroschemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectRootImpl;
import com.gentics.mesh.core.data.root.impl.ProjectSchemaContainerRootImpl;
import com.gentics.mesh.core.data.root.impl.BranchRootImpl;
import com.gentics.mesh.core.data.root.impl.TagFamilyRootImpl;
import com.gentics.mesh.core.endpoint.admin.consistency.ConsistencyCheck;
import com.gentics.mesh.core.rest.admin.consistency.ConsistencyCheckResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;

/**
 * Project specific checks.
 */
public class ProjectCheck implements ConsistencyCheck {

	@Override
	public void invoke(LegacyDatabase db, ConsistencyCheckResponse response, boolean attemptRepair) {
		Iterator<? extends Project> it = db.getVerticesForType(ProjectImpl.class);
		while (it.hasNext()) {
			checkProject(it.next(), response);
		}
	}

	private void checkProject(Project project, ConsistencyCheckResponse response) {
		String uuid = project.getUuid();

		checkIn(project, HAS_PROJECT, ProjectRootImpl.class, response, HIGH);

		checkOut(project, HAS_BRANCH_ROOT, BranchRootImpl.class, response, HIGH);
		checkOut(project, HAS_NODE_ROOT, NodeRootImpl.class, response, HIGH);
		checkOut(project, HAS_TAGFAMILY_ROOT, TagFamilyRootImpl.class, response, HIGH);
		checkOut(project, HAS_ROOT_NODE, NodeImpl.class, response, HIGH);
		checkOut(project, HAS_SCHEMA_ROOT, ProjectSchemaContainerRootImpl.class, response, HIGH);
		checkOut(project, HAS_MICROSCHEMA_ROOT, ProjectMicroschemaContainerRootImpl.class, response, HIGH);

		// checkOut(project, HAS_CREATOR, UserImpl.class, response, MEDIUM);
		// checkOut(project, HAS_EDITOR, UserImpl.class, response, MEDIUM);

		if (isEmpty(project.getName())) {
			response.addInconsistency("Project name is empty or not set", uuid, HIGH);
		}
		if (project.getCreationTimestamp() == null) {
			response.addInconsistency("The project creation date is not set", uuid, MEDIUM);
		}
		if (project.getLastEditedTimestamp() == null) {
			response.addInconsistency("The project edit timestamp is not set", uuid, MEDIUM);
		}
	}

}
