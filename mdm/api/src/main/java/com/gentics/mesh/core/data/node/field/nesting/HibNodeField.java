package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.rest.node.field.NodeField;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public interface HibNodeField extends HibListableField {

	Logger log = LoggerFactory.getLogger(HibNodeField.class);

	/**
	 * Returns the node for this field.
	 *
	 * @return Node for this field when set, otherwise null.
	 */
	HibNode getNode();

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
	Stream<? extends HibNodeFieldContainer> getReferencingContents();

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
