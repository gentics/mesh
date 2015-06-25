package com.gentics.mesh.core.rest.node.response;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.core.rest.common.response.AbstractPropertyContainerModel;
import com.gentics.mesh.core.rest.schema.response.SchemaReference;
import com.gentics.mesh.core.rest.tag.response.TagResponse;

public class NodeResponse extends AbstractPropertyContainerModel {

	private boolean publish;

	private String path;

	private String parentNodeUuid;

	private List<TagResponse> tags = new ArrayList<>();

	private List<String> children;

	private SchemaReference schema;

	private boolean isContainer;

	private String version;

	private String displayField;

	private String segmentField;

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

	public boolean isPublish() {
		return publish;
	}

	public void setPublish(boolean publish) {
		this.publish = publish;
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
}
