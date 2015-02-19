package com.gentics.cailun.core.rest.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.cailun.core.rest.model.Project;
import com.gentics.cailun.core.rest.model.auth.AuthRelationships;
import com.gentics.cailun.core.rest.model.auth.GraphPermission;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.core.rest.model.relationship.BasicRelationships;
import com.gentics.cailun.core.rest.model.relationship.Locked;

/**
 * This class represents a basic cailun node. All models that make use of this model will automatically be able to be handled by the permission system.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

	@RelatedTo(type = BasicRelationships.ASSIGNED_TO_PROJECT, direction = Direction.OUTGOING, elementClass = Project.class)
	protected Project project;

	@RelatedTo(type = BasicRelationships.HAS_CREATOR, direction = Direction.OUTGOING, elementClass = User.class)
	protected User creator;

	@RelatedToVia(type = AuthRelationships.HAS_PERMISSION, direction = Direction.INCOMING, elementClass = GraphPermission.class)
	protected Set<GraphPermission> permissions = new HashSet<>();

	@Fetch
	@RelatedToVia(type = BasicRelationships.IS_LOCKED, direction = Direction.OUTGOING, elementClass = Locked.class)
	protected Locked locked;

	public boolean addPermission(GraphPermission permission) {
		return permissions.add(permission);
	}

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Project getProject() {
		return project;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public boolean isLocked() {
		return locked.isValidLock();
	}

}