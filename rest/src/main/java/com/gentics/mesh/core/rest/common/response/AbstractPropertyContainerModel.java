package com.gentics.mesh.core.rest.common.response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.core.rest.user.response.UserResponse;

public class AbstractPropertyContainerModel extends AbstractRestModel {

	private String language;
	private List<String> availableLanguages;

	private Map<String, String> properties = new HashMap<>();

	private SchemaReference schema;

	private String[] permissions = {};

	private long order = 0;

	private UserResponse creator;
	private long created;

	private UserResponse editor;
	private long edited;

	private ProjectResponse project;

	public AbstractPropertyContainerModel() {
	}

	public UserResponse getCreator() {
		return creator;
	}

	public void setCreator(UserResponse author) {
		this.creator = author;
	}

	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	public SchemaReference getSchema() {
		return schema;
	}

	public long getOrder() {
		return order;
	}

	public void setOrder(long order) {
		this.order = order;
	}

	public String[] getPermissions() {
		return permissions;
	}

	public void setPermissions(String... permissions) {
		this.permissions = permissions;
	}

	/**
	 * Return all properties that were loaded.
	 * 
	 * @return
	 */
	public Map<String, String> getProperties() {
		return properties;
	}

	/**
	 * Return the property for the given key.
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		return properties.get(key);

	}

	/**
	 * Add a property to the set of properties.
	 * 
	 * @param languageKey
	 * @param key
	 * @param value
	 */
	public void addProperty(String key, String value) {
		properties.put(key, value);
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

	public ProjectResponse getProject() {
		return project;
	}

	public void setProject(ProjectResponse project) {
		this.project = project;
	}
}
