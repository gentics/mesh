package com.gentics.mesh.core.data.node;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.GenericVertex;
import com.gentics.mesh.core.data.IndexedVertex;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;

public interface Node extends GenericVertex<NodeResponse>, IndexedVertex {

	public static final String TYPE = "node";

	/**
	 * Add the given tag to the list of tags for this node.
	 * 
	 * @param tag
	 */
	void addTag(Tag tag);

	/**
	 * Remove the given tag from the list of tags for this node.
	 * 
	 * @param tag
	 */
	void removeTag(Tag tag);

	/**
	 * Return a list of tags that were assigned to this node.
	 * 
	 * @return
	 */
	List<? extends Tag> getTags();

	/**
	 * Return the schema container that holds the schema that is used in combination with this node.
	 * 
	 * @return
	 */
	SchemaContainer getSchemaContainer();

	/**
	 * Set the schema container that is used in combination with this node.
	 * 
	 * @param schema
	 */
	void setSchemaContainer(SchemaContainer schema);

	/**
	 * Shortcut method for getSchemaContainer().getSchema()
	 * 
	 * @return
	 * @throws IOException
	 */
	Schema getSchema();

	/**
	 * Return the field container for the given language.
	 * 
	 * @param language
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Language language);

	NodeGraphFieldContainer getOrCreateGraphFieldContainer(Language language);

	List<? extends NodeGraphFieldContainer> getGraphFieldContainers();

	Page<? extends Tag> getTags(ActionContext ac) throws InvalidArgumentException;

	void createLink(Node node);

	List<String> getAvailableLanguageNames();

	Project getProject();

	void setProject(Project project);

	/**
	 * Return the list of children for this node.
	 * 
	 * @return
	 */
	List<? extends Node> getChildren();

	/**
	 * Returns the parentnode of this node.
	 * 
	 * @return
	 */
	Node getParentNode();

	void setParentNode(Node parentNode);

	Node create(User creator, SchemaContainer schemaContainer, Project project);

	Page<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingInfo pagingInfo) throws InvalidArgumentException;

	void setBinaryFileName(String filenName);

	String getBinaryFileName();

	String getBinaryContentType();

	void setBinaryContentType(String contentType);

	/**
	 * Return future that holds a buffer reference to the binary file data.
	 * 
	 * @return
	 */
	Future<Buffer> getBinaryFileBuffer();

	/**
	 * Set the binary file size in bytes
	 * 
	 * @param sizeInBytes
	 */
	void setBinaryFileSize(long sizeInBytes);

	/**
	 * Return the binary file size in bytes
	 * 
	 * @return
	 */
	long getBinaryFileSize();

	void setBinarySHA512Sum(String sha512HashSum);

	String getBinarySHA512Sum();

	void setBinaryImageDPI(Integer dpi);

	Integer getBinaryImageDPI();

	Integer getBinaryImageHeight();

	void setBinaryImageWidth(Integer width);

	/**
	 * Return the width of the binary image
	 * 
	 * @return
	 */
	Integer getBinaryImageWidth();

	/**
	 * Set the with of the binary image. You can set this null to indicate that the binary data has no height.
	 * 
	 * @param heigth
	 */
	void setBinaryImageHeight(Integer heigth);

	/**
	 * Returns the segmented path that points to the binary file within the binary file location. The segmented path is build using the uuid of the node.
	 * 
	 * @return
	 */
	String getSegmentedPath();

	/**
	 * Returns the i18n display name for the node.
	 * 
	 * @param ac
	 * @return
	 */
	String getDisplayName(ActionContext ac);

	NodeGraphFieldContainer findNextMatchingFieldContainer(ActionContext ac);

	String getFilePath();

	void setPublished(boolean published);

	boolean isPublished();

}
