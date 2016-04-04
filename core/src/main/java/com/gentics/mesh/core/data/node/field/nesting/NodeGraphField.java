package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.field.Field;

import rx.Observable;

public interface NodeGraphField extends ListableReferencingGraphField, MicroschemaListableGraphField {

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
	 * @param languageTags list of language tags
	 * @param level Level of transformation
	 */
	Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

}
