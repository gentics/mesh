package com.gentics.mesh.core.data.node;

import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.List;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.util.InvalidArgumentException;

public interface Node extends ContainerNode<NodeResponse> {

	public static final String TYPE = "node";

	void addTag(Tag tag);

	void removeTag(Tag tag);

	SchemaContainer getSchemaContainer();

	Schema getSchema() throws IOException;

	void setSchemaContainer(SchemaContainer schema);

	NodeFieldContainer getFieldContainer(Language language);

	List<? extends Tag> getTags();

	NodeFieldContainer getOrCreateFieldContainer(Language language);

	List<? extends NodeFieldContainer> getFieldContainers();

	Page<? extends Tag> getTags(RoutingContext rc) throws InvalidArgumentException;

	void createLink(Node node);

	NodeImpl getImpl();

	List<String> getAvailableLanguageNames();

}
