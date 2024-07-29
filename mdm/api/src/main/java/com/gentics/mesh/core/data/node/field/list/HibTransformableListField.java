package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.node.field.Field;

public interface HibTransformableListField<RM extends Field> {

	/**
	 * Transform the list to the rest model.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param languageTags
	 *            list of language tags
	 * @param level
	 *            Level of transformation
	 */
	RM transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);
}
