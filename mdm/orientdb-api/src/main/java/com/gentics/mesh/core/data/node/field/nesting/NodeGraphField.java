package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.NodeField;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A {@link NodeGraphField} is a field which is modeled using an edge instead of a dedicated vertex.
 */
public interface NodeGraphField extends ListableReferencingGraphField, MicroschemaListableGraphField {

	Logger log = LoggerFactory.getLogger(NodeGraphField.class);

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
	NodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

	/**
	 * Loads the content that is referencing another node.
	 * @return
	 */
	Stream<? extends NodeGraphFieldContainer> getReferencingContents();

	/**
	 * Gets the name of the field where the node reference originated.
	 * @return
	 */
	String getFieldName();

	/**
	 * Gets the name of the field in the micronode where the node reference originated.
	 * Empty if the reference did not originate from a micronode.
	 * @return
	 */
	Optional<String> getMicronodeFieldName();
}
