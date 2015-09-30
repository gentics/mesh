package com.gentics.mesh.core.rest.node;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.AbstractGenericNodeRestModel;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagFamilyTagGroup;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;

public class NodeResponse extends AbstractGenericNodeRestModel implements NodeField, NodeFieldListItem, NodeReference {

	private String language;

	private List<String> availableLanguages;

	private String path;

	private String version;

	private NodeReferenceImpl parentNode;

	private Map<String, TagFamilyTagGroup> tags = new HashMap<>();

	private ProjectResponse project;

	private List<String> children;

	private SchemaReference schema;

	private boolean published = false;

	private boolean isContainer;

	private String displayField;

	private String segmentField;

	private BinaryProperties binaryProperties;

	private String fileName;

	private FieldMap fields = new FieldMapImpl();

	public NodeResponse() {
	}

	/**
	 * Return the parent node reference for the node. Note that no reference will be set for the base node of a project. This node has no parent. In those cases
	 * the reference will be set to null.
	 * 
	 * @return
	 */
	public NodeReferenceImpl getParentNode() {
		return parentNode;
	}

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

	public List<String> getChildren() {
		return children;
	}

	public void setChildren(List<String> children) {
		this.children = children;
	}

	/**
	 * Return the flag which indicates whether the node is a container.
	 * 
	 * @return
	 */
	public boolean isContainer() {
		return isContainer;
	}

	/**
	 * Set the container flag which indicates whether the node is a container for other nodes. (eg. a folder)
	 * 
	 * @param isContainer
	 */
	public void setContainer(boolean isContainer) {
		this.isContainer = isContainer;
	}

	/**
	 * Return the published flag of the node.
	 * 
	 * @return
	 */
	public boolean isPublished() {
		return published;
	}

	/**
	 * Set the published flag of the node.
	 * 
	 * @param published
	 */
	public void setPublished(boolean published) {
		this.published = published;
	}

	/**
	 * Return the path of the node.
	 * 
	 * @return
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Set the path of the node.
	 * 
	 * @param path
	 */
	public void setPath(String path) {
		this.path = path;
	}

	/**
	 * Return the version of the node.
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version of the node.
	 * 
	 * @param version
	 */
	public void setVersion(String version) {
		this.version = version;
	}

	/**
	 * Return the display field name for the node.
	 * 
	 * @return
	 */
	public String getDisplayField() {
		return displayField;
	}

	/**
	 * Set the display field value for the node.
	 * 
	 * @param displayField
	 */
	public void setDisplayField(String displayField) {
		this.displayField = displayField;
	}

	public BinaryProperties getBinaryProperties() {
		return binaryProperties;
	}

	public void setBinaryProperties(BinaryProperties binaryProperties) {
		this.binaryProperties = binaryProperties;
	}

	/**
	 * Return the binary filename of the node (may be null when no binary value was set)
	 * 
	 * @return
	 */
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
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

	@SuppressWarnings("unchecked")
	public <T extends Field> T getField(String key, Class<T> classOfT) {
		return (T) getFields().get(key);
	}

	@SuppressWarnings("unchecked")
	public <T extends Field> T getField(String key) {
		return (T) getFields().get(key);
	}

	public Map<String, Field> getFields() {
		return fields;
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

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}

}
