package com.gentics.mesh.core.rest.node.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.response.AbstractRestModel;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.core.rest.tag.response.TagResponse;
import com.gentics.mesh.core.rest.user.response.UserResponse;

public class NodeResponse extends AbstractRestModel {

	private boolean published;

	private String language;
	private List<String> availableLanguages;

	private String[] permissions = {};

	private UserResponse creator;
	private long created;

	private UserResponse editor;
	private long edited;

	private String path;

	private String version;

	private String parentNodeUuid;

	private List<TagResponse> tags = new ArrayList<>();

	private ProjectResponse project;

	private List<String> children;

	private SchemaReference schema;

	private boolean isContainer;

	private String displayField;

	private String segmentField;

	private List<Field> fields = new ArrayList<>();

	public NodeResponse() {
	}

	public String getParentNodeUuid() {
		return parentNodeUuid;
	}

	public void setParentNodeUuid(String parentNodeUuid) {
		this.parentNodeUuid = parentNodeUuid;
	}

	public List<TagResponse> getTags() {
		return tags;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	public boolean isContainer() {
		return isContainer;
	}

	public void setContainer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	public boolean isPublished() {
		return published;
	}

	public void setPublished(boolean published) {
		this.published = published;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getDisplayField() {
		return displayField;
	}

	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	public String getSegmentField() {
		return segmentField;
	}

	public void setSegmentField(String segmentField) {
		this.segmentField = segmentField;
	}

	public ProjectResponse getProject() {
		return project;
	}

	public void setProject(ProjectResponse project) {
		this.project = project;
	}

	public List<Field> getFields() {
		return fields;
	}

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse author) {
		this.creator = author;
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public List<String> getAvailableLanguages() {
		return availableLanguages;
	}

	public void setAvailableLanguages(List<String> availableLanguages) {
		this.availableLanguages = availableLanguages;
	}

	public long getCreated() {
		return created;
	}

	public void setCreated(long created) {
		this.created = created;
	}

	public UserResponse getEditor() {
		return editor;
	}

	public void setEditor(UserResponse editor) {
		this.editor = editor;
	}

	public long getEdited() {
		return edited;
	}

	public void setEdited(long edited) {
		this.edited = edited;
	}

}
