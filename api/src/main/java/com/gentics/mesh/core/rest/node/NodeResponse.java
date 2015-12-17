package com.gentics.mesh.core.rest.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;

/**
 * POJO for the node rest response model.
 */
public class NodeResponse extends AbstractGenericRestResponse implements NodeField, NodeFieldListItem, NodeReference {

	private String language;

	private List<String> availableLanguages;

	private NodeReferenceImpl parentNode;

	private Map<String, TagFamilyTagGroup> tags = new HashMap<>();

	private ProjectResponse project;

	private Map<String, NodeChildrenInfo> childrenInfo = new HashMap<>();

	private SchemaReference schema;

	private boolean published = false;

	private boolean isContainer;

	private String displayField;

	private FieldMap fields = new FieldMapImpl();

	private String url;

	public NodeResponse() {
	}

	/**
	 * Return the parent node reference for the node. Note that no reference will be set for the base node of a project. This node has no parent. In those cases
	 * the reference will be set to null.
	 * 
	 * @return Node reference
	 */
	public NodeReferenceImpl getParentNode() {
		return parentNode;
	}

	/**
	 * Set parent node reference
	 * 
	 * @param parentNode
	 *            Parent node reference
	 */
	public void setParentNode(NodeReferenceImpl parentNode) {
		this.parentNode = parentNode;
	}

	/**
	 * Return the tags which were used to tag the node. The tags are nested within their tag families.
	 * 
	 * @return
	 */
	public Map<String, TagFamilyTagGroup> getTags() {
		return tags;
	}

	/**
	 * Set the schema reference of the node.
	 * 
	 * @param schema
	 */
	public void setSchema(SchemaReference schema) {
		this.schema = schema;
	}

	/**
	 * Return the schema reference of the node.
	 * 
	 * @return
	 */
	public SchemaReference getSchema() {
		return schema;
	}

	/**
	 * Return the children node info element.
	 * 
	 * @return
	 */
	public Map<String, NodeChildrenInfo> getChildrenInfo() {
		return childrenInfo;
	}

	/**
	 * Return the flag which indicates whether the node is a container.
	 * 
	 * @return Container flag
	 */
	public boolean isContainer() {
		return isContainer;
	}

	/**
	 * Set the container flag which indicates whether the node is a container for other nodes. (eg. a folder)
	 * 
	 * @param isContainer
	 *            Container flag
	 */
	public void setContainer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	/**
	 * Return the published flag of the node.
	 * 
	 * @return Published flag
	 */
	public boolean isPublished() {
		return published;
	}

	/**
	 * Set the published flag of the node.
	 * 
	 * @param published
	 *            Published flag
	 */
	public void setPublished(boolean published) {
		this.published = published;
	}

	/**
	 * Return the display field name for the node.
	 * 
	 * @return Display field
	 */
	public String getDisplayField() {
		return displayField;
	}

	/**
	 * Set the display field value for the node.
	 * 
	 * @param displayField
	 *            Display field
	 */
	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	/**
	 * Return the project to which the node belongs.
	 * 
	 * @return Project of the node
	 */
	public ProjectResponse getProject() {
		return project;
	}

	/**
	 * Set the project to which the node belongs.
	 * 
	 * @param project
	 *            Project of the node
	 */
	public void setProject(ProjectResponse project) {
		this.project = project;
	}

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 *            Key of the field to be returned
	 * @param classOfT
	 *            Class of the field
	 * @return Field or null of no field could be found for the given key
	 */
	@SuppressWarnings("unchecked")
	public <T extends Field> T getField(String key, Class<T> classOfT) {
		return (T) getFields().get(key);
	}

	/**
	 * Return the field with the given key.
	 * 
	 * @param key
	 *            Name of the field
	 * @return Found field or null when no field could be found
	 * @param <T>
	 *            Class of the field
	 */
	@SuppressWarnings("unchecked")
	public <T extends Field> T getField(String key) {
		return (T) getFields().get(key);
	}

	/**
	 * Return a map with fields of the node.
	 * 
	 * @return Map with fields
	 */
	public Map<String, Field> getFields() {
		return fields;
	}

	/**
	 * Return the language tag of the node.
	 * 
	 * @return Language tag
	 */
	public String getLanguage() {
		return language;
	}

	/**
	 * Set the language of the node.
	 * 
	 * @param language
	 *            Language tag
	 */
	public void setLanguage(String language) {
		this.language = language;
	}

	/**
	 * Return a list of language tags which are available for the node.
	 * 
	 * @return List of language tags
	 */
	public List<String> getAvailableLanguages() {
		return availableLanguages;
	}

	/**
	 * Set a list of language tags which are available for the node.
	 * 
	 * @param availableLanguages
	 *            List of language tags
	 */
	public void setAvailableLanguages(List<String> availableLanguages) {
		this.availableLanguages = availableLanguages;
	}

	@Override
	public String getUrl() {
		return url;
	}

	/**
	 * Set the webroot URL
	 * 
	 * @param url
	 *            webroot URL
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}

}
