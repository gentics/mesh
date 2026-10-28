package com.gentics.mesh.plugin.common.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.node.FieldMap;
import com.gentics.mesh.core.rest.node.NodeChildrenInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.PublishStatusModel;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base class for plugin specific models which wrap {@link NodeResponse NodeResponses}.
 */
public class NodeResponseDelegate implements JsonSerializable {

	private final NodeResponse delegate;

	public NodeResponseDelegate(NodeResponse delegate) {
		this.delegate = delegate;
	}

	public NodeResponse getDelegate() {
		return delegate;
	}

	public NodeReference getParentNode() {
		return delegate.getParentNode();
	}

	public void setParentNode(NodeReference parentNode) {
		delegate.setParentNode(parentNode);
	}

	public List<TagReference> getTags() {
		return delegate.getTags();
	}

	public void setSchema(SchemaReference schema) {
		delegate.setSchema(schema);
	}

	public SchemaReference getSchema() {
		return delegate.getSchema();
	}

	public Map<String, NodeChildrenInfo> getChildrenInfo() {
		return delegate.getChildrenInfo();
	}

	public void setChildrenInfo(Map<String, NodeChildrenInfo> childrenInfo) {
		delegate.setChildrenInfo(childrenInfo);
	}

	public boolean isContainer() {
		return delegate.getContainer();
	}

	public void setContainer(boolean isContainer) {
		delegate.setContainer(isContainer);
	}

	public String getDisplayField() {
		return delegate.getDisplayField();
	}

	public NodeResponse setDisplayField(String displayField) {
		return delegate.setDisplayField(displayField);
	}

	public String getDisplayName() {
		return delegate.getDisplayName();
	}

	public void setDisplayName(String displayName) {
		delegate.setDisplayName(displayName);
	}

	public ProjectReference getProject() {
		return delegate.getProject();
	}

	public void setProject(ProjectReference project) {
		delegate.setProject(project);
	}

	public FieldMap getFields() {
		return delegate.getFields();
	}

	public String getLanguage() {
		return delegate.getLanguage();
	}

	public void setLanguage(String language) {
		delegate.setLanguage(language);
	}

	public Map<String, PublishStatusModel> getAvailableLanguages() {
		return delegate.getAvailableLanguages();
	}

	public void setAvailableLanguages(Map<String, PublishStatusModel> availableLanguages) {
		delegate.setAvailableLanguages(availableLanguages);
	}

	public Map<String, String> getLanguagePaths() {
		return delegate.getLanguagePaths();
	}

	public void setLanguagePaths(Map<String, String> languagePaths) {
		delegate.setLanguagePaths(languagePaths);
	}

	public String getPath() {
		return delegate.getPath();
	}

	public NodeResponse setPath(String path) {
		return delegate.setPath(path);
	}

	public List<NodeReference> getBreadcrumb() {
		return delegate.getBreadcrumb();
	}

	public NodeResponse setBreadcrumb(List<NodeReference> breadcrumb) {
		return delegate.setBreadcrumb(breadcrumb);
	}

	public String getVersion() {
		return delegate.getVersion();
	}

	public NodeResponse setVersion(String version) {
		return delegate.setVersion(version);
	}

	@JsonIgnore
	public String getType() {
		return delegate.getType();
	}

	public UserReference getCreator() {
		return delegate.getCreator();
	}

	public void setCreator(UserReference creator) {
		delegate.setCreator(creator);
	}

	public String getCreated() {
		return delegate.getCreated();
	}

	public void setCreated(String created) {
		delegate.setCreated(created);
	}

	public UserReference getEditor() {
		return delegate.getEditor();
	}

	public void setEditor(UserReference editor) {
		delegate.setEditor(editor);
	}

	public String getEdited() {
		return delegate.getEdited();
	}

	public void setEdited(String edited) {
		delegate.setEdited(edited);
	}

	public PermissionInfo getPermissions() {
		return delegate.getPermissions();
	}

	public void setPermissions(PermissionInfo permissions) {
		delegate.setPermissions(permissions);
	}

	@JsonIgnore
	public void setPermissions(Permission... permissions) {
		delegate.setPermissions(permissions);
	}

	public PermissionInfo getRolePerms() {
		return delegate.getRolePerms();
	}

	public void setRolePerms(PermissionInfo rolePerms) {
		delegate.setRolePerms(rolePerms);
	}

	@JsonIgnore
	public void setRolePerms(Permission... permissions) {
		delegate.setRolePerms(permissions);
	}

	public String getUuid() {
		return delegate.getUuid();
	}

	public void setUuid(String uuid) {
		delegate.setUuid(uuid);
	}

	@Override
	public String toJson() {
		return delegate.toJson();
	}

	protected Optional<Boolean> getOptionalBool(String fieldName) {
		return Optional.ofNullable(getFields().getBooleanField(fieldName)).map(BooleanFieldImpl::getValue);
	}

	protected Optional<List<String>> getOptionalStringList(String fieldName) {
		return Optional.ofNullable(getFields().getStringFieldList(fieldName)).map(StringFieldListImpl::getItems);
	}

	protected void setBool(String fieldName, boolean value) {
		getFields().put(fieldName, new BooleanFieldImpl().setValue(value));
	}

	protected void setStringList(String fieldName, List<String> value) {
		getFields().put(fieldName, new StringFieldListImpl().setItems(value));
	}

	protected boolean getBool(String fieldName) {
		return getFields().getBooleanField(fieldName).getValue();
	}

	protected void setString(String fieldName, String value) {
		getFields().put(fieldName, new StringFieldImpl().setString(value));
	}

	protected String getString(String fieldName) {
		return getFields().getStringField(fieldName).getString();
	}

	protected List<String> getStringList(String fieldName) {
		return getFields().getStringFieldList(fieldName).getItems();
	}

	protected Optional<String> getOptionalString(String fieldName) {
		return Optional.ofNullable(getFields().getStringField(fieldName)).map(StringFieldImpl::getString);
	}

}
