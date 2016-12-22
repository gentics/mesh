package com.gentics.mesh.core.data.node.field.list;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.concurrent.atomic.AtomicInteger;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
import com.gentics.mesh.dagger.MeshInternal;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface NodeGraphFieldList extends ListGraphField<NodeGraphField, NodeFieldList, Node> {

	final Logger log = LoggerFactory.getLogger(NodeGraphFieldList.class);

	String TYPE = "node";
	
	NodeGraphField createNode(String key, Node node);

}
