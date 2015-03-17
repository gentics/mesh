package com.gentics.cailun.core.data.model.generic;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;
import org.springframework.data.neo4j.annotation.RelatedToVia;

import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.relationship.BasicRelationships;
import com.gentics.cailun.core.data.model.relationship.Locked;

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
	protected Set<Project> projects = new HashSet<>();

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_CREATOR, direction = Direction.OUTGOING, elementClass = User.class)
	protected User creator;

	@Fetch
	@RelatedToVia(type = BasicRelationships.IS_LOCKED, direction = Direction.OUTGOING, elementClass = Locked.class)
	protected Locked locked;

	public User getCreator() {
		return creator;
	}

	public void setCreator(User creator) {
		this.creator = creator;
	}

	public Set<Project> getProjects() {
		return projects;
	}

	public boolean addProject(Project project) {
		return this.projects.add(project);
	}

	public boolean removeProject(Project project) {
		return this.projects.remove(project);
	}

	public boolean isLocked() {
		if (locked == null) {
			return false;
		} else {
			return locked.isValidLock();
		}
	}

}