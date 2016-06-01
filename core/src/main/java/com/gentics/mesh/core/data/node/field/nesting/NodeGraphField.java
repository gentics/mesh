package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.FieldGetter;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.data.node.field.FieldUpdater;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public interface NodeGraphField extends ListableReferencingGraphField, MicroschemaListableGraphField {

	static final Logger log = LoggerFactory.getLogger(NodeGraphField.class);

	FieldTransformator NODE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		NodeGraphField graphNodeField = container.getNode(fieldKey);
		if (graphNodeField == null) {
			return Observable.just(null);
		} else {
			return graphNodeField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	FieldUpdater NODE_UPDATER = (container, ac, fieldMap, fieldKey, fieldSchema, schema) -> {
		NodeGraphField graphNodeField = container.getNode(fieldKey);
		NodeField nodeField = fieldMap.getNodeField(fieldKey);
		boolean isNodeFieldSetToNull = fieldMap.hasField(fieldKey) && (nodeField == null);
		GraphField.failOnDeletionOfRequiredField(graphNodeField, isNodeFieldSetToNull, fieldSchema, fieldKey, schema);
		boolean restIsNullOrEmpty = nodeField == null;
		GraphField.failOnMissingRequiredField(graphNodeField, restIsNullOrEmpty, fieldSchema, fieldKey, schema);

		// Handle Deletion - Remove the field if the field has been explicitly set to null
		if (graphNodeField != null && isNodeFieldSetToNull) {
			graphNodeField.removeField(container);
			return;
		}

		// Rest model is empty or null - Abort
		if (restIsNullOrEmpty) {
			return;
		}

		// Handle Update / Create 
		BootstrapInitializer boot = BootstrapInitializer.getBoot();
		Observable<Node> obsNode = boot.nodeRoot().findByUuid(nodeField.getUuid());
		obsNode.map(node -> {
			if (node == null) {
				// TODO We want to delete the field when the field has been explicitly set to null
				if (log.isDebugEnabled()) {
					log.debug("Node field {" + fieldKey + "} could not be populated since node {" + nodeField.getUuid() + "} could not be found.");
				}
				// TODO we need to fail here - the node could not be found.
				// throw error(NOT_FOUND, "The field {, parameters)
			} else {
				// Check whether the container already contains a node field
				// TODO check node permissions
				if (graphNodeField == null) {
					container.createNode(fieldKey, node);
				} else {
					// We can't update the graphNodeField since it is in
					// fact an edge. We need to delete it and create a new
					// one.
					container.deleteFieldEdge(fieldKey);
					container.createNode(fieldKey, node);
				}
			}
			return null;
		}).toBlocking().single();
	};

	FieldGetter NODE_GETTER = (container, fieldSchema) -> {
		return container.getNode(fieldSchema.getName());
	};

	/**
	 * Returns the node for this field.
	 * 
	 * @return Node for this field when set, otherwise null.
	 */
	Node getNode();

	/**
	 * Transform the graph field into a rest field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param languageTags
	 *            list of language tags
	 * @param level
	 *            Level of transformation
	 */
	Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

}
