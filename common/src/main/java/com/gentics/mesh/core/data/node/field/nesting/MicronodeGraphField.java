package com.gentics.mesh.core.data.node.field.nesting;

import java.util.List;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.GraphFieldContainer;
import com.gentics.mesh.core.data.MeshEdge;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.rest.node.field.MicronodeField;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A {@link MicronodeGraphField} is an {@link MeshEdge} which links a {@link GraphFieldContainer} to a {@link Micronode} vertex.
 */
public interface MicronodeGraphField extends ListableReferencingGraphField, MeshEdge {

	Logger log = LoggerFactory.getLogger(MicronodeGraphField.class);

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
	MicronodeField transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags, int level);

}
