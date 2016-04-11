package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.FieldTransformator;
import com.gentics.mesh.core.rest.micronode.NullMicronodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;

import rx.Observable;

public interface MicronodeGraphField extends ListableReferencingGraphField {

	FieldTransformator MICRONODE_TRANSFORMATOR = (container, ac, fieldKey, fieldSchema, languageTags, level, parentNode) -> {
		MicronodeGraphField micronodeGraphField = container.getMicronode(fieldKey);
		if (micronodeGraphField == null) {
			return Observable.just(new NullMicronodeResponse());
		} else {
			return micronodeGraphField.transformToRest(ac, fieldKey, languageTags, level);
		}
	};

	/**
	 * Returns the micronode for this field.
	 * 
	 * @return Micronode for this field when set, otherwise null.
	 */
	Micronode getMicronode();

	/**
	 * Transform the graph field into a rest field.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param languageTags
	 *            language tags
	 * @param level
	 *            Level of transformation
	 */
	Observable<? extends Field> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);
}
