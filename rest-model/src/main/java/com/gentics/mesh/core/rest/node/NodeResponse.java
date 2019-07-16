package com.gentics.mesh.core.rest.node;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.common.AbstractGenericRestResponse;
import com.gentics.mesh.core.rest.common.AbstractResponse;
import com.gentics.mesh.core.rest.common.FieldContainer;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.ExpandableNode;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.parameter.NodeParameters;

/**
 * POJO for the node rest response model.
 */
public class NodeResponse extends AbstractGenericRestResponse implements NodeField, NodeFieldListItem, ExpandableNode, FieldContainer {

	@JsonProperty(required = false)
	@JsonPropertyDescription("ISO 639-1 language tag of the node content.")
	private String language;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Map of languages for which content is available and their publish status.")
	private Map<String, PublishStatusModel> availableLanguages;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Map of webroot paths per language. This property will only be populated if the "
		+ NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY + " query parameter has been set accordingly.")
	private Map<String, String> languagePaths;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Reference to the parent node. Note that the root node of the project has no parent node.")
	private NodeReference parentNode;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of tags that were used to tag the node.")
	private List<TagReference> tags;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the project of the node.")
	private ProjectReference project;

	/**
	 * Key: Schema name, Value: Node information
	 */
	@JsonProperty(required = false)
	@JsonPropertyDescription("Object which contains information about child elements.")
	private Map<String, NodeChildrenInfo> childrenInfo;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the node.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference schema;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Flag which indicates whether the node is a container and can contain nested elements.")
	private Boolean container;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Display field name of the node. May not be retured if the node schema has no display field.")
	private String displayField;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Display field value of the node. May not be retured if the node schema has no display field.")
	private String displayName;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Dynamic map with fields of the node language specific content.")
	private FieldMap fields;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Webroot path to the node content. Will only be provided if the " + NodeParameters.RESOLVE_LINKS_QUERY_PARAM_KEY
		+ " query parameter has been set accordingly.")
	private String path;

	@JsonProperty(required = true)
	@JsonPropertyDescription("List of nodes which construct the breadcrumb. Note that the start node will not be included in the list.")
	private List<NodeReference> breadcrumb;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Version of the node content.")
	private String version;

	public NodeResponse() {
	}

	/**
	 * Return the parent node reference for the node. Note that no reference will be set for the base node of a project. This node has no parent. In those cases
	 * the reference will be set to null.
	 * 
	 * @return Node reference
	 */
	public NodeReference getParentNode() {
		return parentNode;
	}

	/**
	 * Set parent node reference
	 * 
	 * @param parentNode
	 *            Parent node reference
	 */
	public void setParentNode(NodeReference parentNode) {
		this.parentNode = parentNode;
	}

	/**
	 * Return the tags which were used to tag the node. The tags are nested within their tag families.
	 * 
	 * @return
	 */
	public List<TagReference> getTags() {
		return tags;
	}

	public void setTags(List<TagReference> tags) {
		this.tags = tags;
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
	 * Set the children node info element.
	 * 
	 * @param childrenInfo
	 */
	public void setChildrenInfo(Map<String, NodeChildrenInfo> childrenInfo) {
		this.childrenInfo = childrenInfo;
	}

	/**
	 * Return the flag which indicates whether the node is a container.
	 * 
	 * @return Container flag
	 * @deprecated Use {@link #getContainer()}
	 */
	@Deprecated
	@JsonIgnore
	public boolean isContainer() {
		return container != null ? container : false;
	}

	/**
	 * Return the flag which indicates whether the node is a container.
	 * 
	 * @return
	 */
	public Boolean getContainer() {
		return container;
	}

	/**
	 * Set the container flag which indicates whether the node is a container for other nodes. (eg. a folder)
	 * 
	 * @param isContainer
	 *            Container flag
	 */
	public void setContainer(Boolean isContainer) {
		this.container = isContainer;
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
	 * Set the display field name for the node.
	 * 
	 * @param displayField
	 *            Display field
	 */
	public NodeResponse setDisplayField(String displayField) {
		this.displayField = displayField;
		return this;
	}

	/**
	 * Return the display field value for the node.
	 * 
	 * @return Display field value
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Set the display field value for the node.
	 * 
	 * @param displayName
	 *            Display field value
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * Return the project to which the node belongs.
	 * 
	 * @return Project of the node
	 */
	public ProjectReference getProject() {
		return project;
	}

	/**
	 * Set the project to which the node belongs.
	 * 
	 * @param project
	 *            Project of the node
	 */
	public void setProject(ProjectReference project) {
		this.project = project;
	}

	/**
	 * Return a map with fields of the node.
	 * 
	 * @return Map with fields
	 */
	public FieldMap getFields() {
		return fields;
	}

	/**
	 * Set the fields of the node.
	 * 
	 * @param fields
	 */
	public void setFields(FieldMap fields) {
		this.fields = fields;
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
	 * Return a map of language tags which are available for the node.
	 * 
	 * @return Map of language tags
	 */
	public Map<String, PublishStatusModel> getAvailableLanguages() {
		return availableLanguages;
	}

	/**
	 * Set a map of language tags which are available for the node and their publish status.
	 * 
	 * @param availableLanguages
	 *            List of language tags
	 */
	public void setAvailableLanguages(Map<String, PublishStatusModel> availableLanguages) {
		this.availableLanguages = availableLanguages;
	}

	/**
	 * Return the language webroot paths for the node. The map key is the language tag and the value is the resolved webroot link.
	 * 
	 * @return
	 */
	public Map<String, String> getLanguagePaths() {
		return languagePaths;
	}

	/**
	 * Set the language webroot paths.
	 * 
	 * @param languagePaths
	 */
	public void setLanguagePaths(Map<String, String> languagePaths) {
		this.languagePaths = languagePaths;
	}

	@Override
	public String getPath() {
		return path;
	}

	/**
	 * Set the webroot path
	 * 
	 * @param path
	 *            webroot path
	 * @return Fluent API
	 */
	public NodeResponse setPath(String path) {
		this.path = path;
		return this;
	}

	/**
	 * Return the breadcrumb of the node. The key contains the uuid of the breadcrumb segment. The value maps to the segment name. The breadcrumb will only
	 * contain parent elements of the node for this response.
	 * 
	 * @return
	 */
	public List<NodeReference> getBreadcrumb() {
		return breadcrumb;
	}

	/**
	 * Set the breadcrumb for the node.
	 * 
	 * @param breadcrumb
	 * @return Fluent API
	 */
	public NodeResponse setBreadcrumb(List<NodeReference> breadcrumb) {
		this.breadcrumb = breadcrumb;
		return this;
	}

	/**
	 * Get the version of the fields
	 * 
	 * @return version number
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Set the version of the fields
	 * 
	 * @param version
	 * @return Fluent API
	 */
	public NodeResponse setVersion(String version) {
		this.version = version;
		return this;
	}

	@JsonIgnore
	@Override
	public String getType() {
		return FieldTypes.NODE.toString();
	}

	/**
	 * Helper method which convert the response into an update request.
	 * 
	 * @return
	 */
	@JsonIgnore
	public NodeUpdateRequest toRequest() {
		NodeUpdateRequest request = new NodeUpdateRequest();
		request.setLanguage(getLanguage());
		request.setVersion(getVersion());
		request.setFields(getFields());
		return request;
	}

	/**
	 * Compares the given object with the node. The uuid, language and version will be utilized to compare two node responses.
	 */
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o instanceof NodeResponse) {
			NodeResponse that = (NodeResponse) o;
			return Objects.equals(getUuid(), that.getUuid())
				&& Objects.equals(getLanguage(), that.getLanguage())
				&& Objects.equals(getVersion(), that.getVersion());
		} else {
			return super.equals(o);
		}
	}

}
