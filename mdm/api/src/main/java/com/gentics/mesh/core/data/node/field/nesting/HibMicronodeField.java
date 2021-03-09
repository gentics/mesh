package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.rest.node.field.MicronodeField;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A {@link MicronodeGraphField} is an {@link MeshEdge} which links a {@link GraphFieldContainer} to a {@link Micronode} vertex.
 */
public interface HibMicronodeField extends HibListableField {

	Logger log = LoggerFactory.getLogger(HibMicronodeField.class);

	/**
	 * Returns the micronode for this field.
	 * 
	 * @return Micronode for this field when set, otherwise null.
	 */
	HibMicronode getMicronode();

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
	MicronodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

}
