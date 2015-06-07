package com.gentics.mesh.core.data.model;

import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.Indexed;
import org.springframework.data.neo4j.annotation.NodeEntity;
import org.springframework.data.neo4j.annotation.RelatedTo;

import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.GroupRoot;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.RoleRoot;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.data.model.auth.UserRoot;
import com.gentics.mesh.core.data.model.generic.AbstractPersistable;
import com.gentics.mesh.core.data.model.relationship.BasicRelationships;
import com.gentics.mesh.core.data.model.schema.MicroSchemaRoot;
import com.gentics.mesh.core.data.model.schema.ObjectSchema;
import com.gentics.mesh.core.data.model.schema.ObjectSchemaRoot;

/**
 * The root element of the whole mesh system. All projects, roles, users etc are connected to this single instance entity.
 * 
 * @author johannes2
 *
 */
@NodeEntity
public class MeshRoot extends AbstractPersistable {

	private static final long serialVersionUID = -901251232180415110L;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_PROJECT_ROOT, direction = Direction.OUTGOING, elementClass = ProjectRoot.class)
	private ProjectRoot projectRoot;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_USER_ROOT, direction = Direction.OUTGOING, elementClass = UserRoot.class)
	private UserRoot userRoot;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_ROLE_ROOT, direction = Direction.OUTGOING, elementClass = RoleRoot.class)
	private RoleRoot roleRoot;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_LANGUAGE_ROOT, direction = Direction.OUTGOING, elementClass = LanguageRoot.class)
	private LanguageRoot languageRoot;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_SCHEMA_ROOT, direction = Direction.OUTGOING, elementClass = ObjectSchemaRoot.class)
	private ObjectSchemaRoot objectSchemaRoot;
	
	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_MICRO_SCHEMA_ROOT, direction = Direction.OUTGOING, elementClass = MicroSchemaRoot.class)
	private MicroSchemaRoot microSchemaRoot;

	@Fetch
	@RelatedTo(type = BasicRelationships.HAS_GROUP_ROOT, direction = Direction.OUTGOING, elementClass = GroupRoot.class)
	private GroupRoot groupRoot;

	@Indexed(unique = true)
	private String unique = MeshRoot.class.getSimpleName();

	public Set<User> getUsers() {
		return userRoot.getUsers();
	}

	public UserRoot getUserRoot() {
		return userRoot;
	}

	public void addUser(User user) {
		this.userRoot.getUsers().add(user);
	}

	public Set<Language> getLanguages() {
		return languageRoot.getLanguages();
	}

	public LanguageRoot getLanguageRoot() {
		return languageRoot;
	}

	public void addLanguage(Language language) {
		this.languageRoot.getLanguages().add(language);
	}

	public Set<Group> getGroups() {
		return groupRoot.getGroups();
	}

	public GroupRoot getGroupRoot() {
		return groupRoot;
	}

	public void addGroup(Group group) {
		this.groupRoot.getGroups().add(group);
	}

	public Set<Role> getRoles() {
		return roleRoot.getRoles();
	}

	public RoleRoot getRoleRoot() {
		return roleRoot;
	}

	public void addRole(Role role) {
		this.roleRoot.getRoles().add(role);
	}

	public Set<ObjectSchema> getSchemas() {
		return objectSchemaRoot.getSchemas();
	}

	public ObjectSchemaRoot getObjectSchemaRoot() {
		return objectSchemaRoot;
	}

	public void addSchema(ObjectSchema schema) {
		this.objectSchemaRoot.getSchemas().add(schema);
	}

	public Set<Project> getProjects() {
		return projectRoot.getProjects();
	}

	public ProjectRoot getProjectRoot() {
		return projectRoot;
	}

	public void addProject(Project project) {
		this.projectRoot.getProjects().add(project);
	}

	public void setProjectRoot(ProjectRoot projectRoot) {
		this.projectRoot = projectRoot;
	}

	public void setGroupRoot(GroupRoot groupRoot) {
		this.groupRoot = groupRoot;
	}

	public void setRoleRoot(RoleRoot roleRoot) {
		this.roleRoot = roleRoot;
	}

	public void setLanguageRoot(LanguageRoot languageRoot) {
		this.languageRoot = languageRoot;
	}

	public void setObjectSchemaRoot(ObjectSchemaRoot objectSchemaRoot) {
		this.objectSchemaRoot = objectSchemaRoot;
	}

	public void setUserRoot(UserRoot userRoot) {
		this.userRoot = userRoot;
	}
	
	public MicroSchemaRoot getMicroSchemaRoot() {
		return microSchemaRoot;
	}
}
