package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.NodeFieldModel;

public interface NodeField extends ListableField, ReferenceField<Node> {

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
	NodeFieldModel transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

	/**
	 * Loads the content that is referencing another node.
	 * @return
	 */
	default Stream<? extends NodeFieldContainer> getReferencingContents() {
		return getReferencingContents(true, true);
	}

	/**
	 * Loads the content that is referencing another node.
	 * 
	 * @param lookupInContent search in node content
	 * @param lookupInMicronode search in micronodes
	 * @return
	 */
	Stream<? extends NodeFieldContainer> getReferencingContents(boolean lookupInContent, boolean lookupInMicronode);

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

	@Override
	default Node getReferencedEntity() {
		return getNode();
	}
}
