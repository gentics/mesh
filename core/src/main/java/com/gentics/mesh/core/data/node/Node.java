package com.gentics.mesh.core.data.node;

import java.util.List;

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
import com.gentics.mesh.core.rest.node.NodeBreadcrumbResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.user.NodeReferenceImpl;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
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
	 */
	Schema getSchema();

	/**
	 * Return the field container for the given language.
	 * 
	 * @param language
	 * @return
	 */
	NodeGraphFieldContainer getGraphFieldContainer(Language language);

	/**
	 * Return the field container for the given language. Create the container when non was found.
	 * 
	 * @param language
	 * @return
	 */
	NodeGraphFieldContainer getOrCreateGraphFieldContainer(Language language);

	/**
	 * Return a list of graph field containers for the node.
	 * 
	 * @return
	 */
	List<? extends NodeGraphFieldContainer> getGraphFieldContainers();

	/**
	 * Return a page of tags that are assigned to the node.
	 * 
	 * @param ac
	 * @return
	 * @throws InvalidArgumentException
	 */
	Page<? extends Tag> getTags(InternalActionContext ac) throws InvalidArgumentException;

	/***
	 * Create link between the nodes.
	 * 
	 * @param node
	 */
	void createLink(Node node);

	/**
	 * Return a list of language names.
	 * 
	 * @return
	 */
	List<String> getAvailableLanguageNames();

	/**
	 * Return the project of the node.
	 * 
	 * @return
	 */
	Project getProject();

	/**
	 * Set the project of the node.
	 * 
	 * @param project
	 */
	void setProject(Project project);

	/**
	 * Return the list of children for this node.
	 * 
	 * @return
	 */
	List<? extends Node> getChildren();

	/**
	 * Returns the parent node of this node.
	 * 
	 * @return
	 */
	Node getParentNode();

	/**
	 * Set the parent node of this node
	 * 
	 * @param parentNode
	 */
	void setParentNode(Node parentNode);

	/**
	 * Create a child node in this node.
	 * 
	 * @param creator
	 * @param schemaContainer
	 * @param project
	 * @return
	 */
	Node create(User creator, SchemaContainer schemaContainer, Project project);

	/**
	 * Return a page with child nodes that are visible to the given user.
	 * 
	 * @param requestUser
	 * @param languageTags
	 * @param pagingParameter
	 * @return
	 * @throws InvalidArgumentException
	 */
	Page<? extends Node> getChildren(MeshAuthUser requestUser, List<String> languageTags, PagingParameter pagingParameter)
			throws InvalidArgumentException;

	/**
	 * Return the binary filename.
	 * 
	 * @return
	 */
	String getBinaryFileName();

	/**
	 * Set the binary filename.
	 * 
	 * @param filenName
	 */
	void setBinaryFileName(String filenName);

	/**
	 * Return the binary content type of the node.
	 * 
	 * @return
	 */
	String getBinaryContentType();

	/**
	 * Set the binary content type of the node.
	 * 
	 * @param contentType
	 */
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

	/**
	 * Set the binary SHA 512 checksum.
	 * 
	 * @param sha512HashSum
	 */
	void setBinarySHA512Sum(String sha512HashSum);

	/**
	 * Return the binary SHA 512 checksum.
	 * 
	 * @return
	 */
	String getBinarySHA512Sum();

	/**
	 * Set the binary image DPI.
	 * 
	 * @param dpi
	 */
	void setBinaryImageDPI(Integer dpi);

	/**
	 * Return the binary image DPI.
	 * 
	 * @return
	 */
	Integer getBinaryImageDPI();

	/**
	 * Return the binary image height.
	 * 
	 * @return
	 */
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
	String getBinarySegmentedPath();

	/**
	 * Returns the i18n display name for the node. The display name will be determined by loading the i18n field value for the display field parameter of the
	 * node's schema. It may be possible that no display name can be returned since new nodes may not have any values.
	 * 
	 * @param ac
	 * @return
	 */
	String getDisplayName(InternalActionContext ac);

	/**
	 * Find a node field container that matches the nearest possible value for the ?lang= request parameter. When a user requests a node using ?lang=de,en and
	 * there is no de version the en version will be selected and returned.
	 * 
	 * @param ac
	 * @return Next matching field container or null when no language matched
	 */
	NodeGraphFieldContainer findNextMatchingFieldContainer(InternalActionContext ac);

	/**
	 * Return the file path for the binary file location of the node.
	 * 
	 * @return
	 */
	String getFilePath();

	/**
	 * Set the published flag.
	 * 
	 * @param published
	 */
	void setPublished(boolean published);

	/**
	 * Return the published flag state.
	 * 
	 * @return
	 */
	boolean isPublished();

	/**
	 * Move this node into the target node.
	 * 
	 * @param ac
	 * @param targetNode
	 * @param handler
	 * @return
	 */
	Node moveTo(InternalActionContext ac, Node targetNode, Handler<AsyncResult<Void>> handler);

	/**
	 * Transform the node into a node reference rest model.
	 * 
	 * @param ac
	 * @param handler
	 */
	Node transformToReference(InternalActionContext ac, Handler<AsyncResult<NodeReferenceImpl>> handler);

	/**
	 * Transform information from the node into a breadcrumb rest model.
	 * 
	 * @param ac
	 * @param handler
	 * @return
	 */
	Node transformToBreadcrumb(InternalActionContext ac, Handler<AsyncResult<NodeBreadcrumbResponse>> handler);

	/**
	 * Delete the language container for the given language.
	 * 
	 * @param ac
	 * @param language
	 * @param handler
	 * @return
	 */
	Node deleteLanguageContainer(InternalActionContext ac, Language language, Handler<AsyncResult<Void>> handler);

	/**
	 * Return the path segment of this node.
	 * 
	 * @return
	 */
	String getPathSegment(InternalActionContext ac);

	/**
	 * Return the full path to this node.
	 * 
	 * @param ac
	 * @return
	 */
	String getPath(InternalActionContext ac);

}
