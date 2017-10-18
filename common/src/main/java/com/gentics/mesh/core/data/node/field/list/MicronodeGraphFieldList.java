package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;

import io.reactivex.Single;

/**
 * Graph field which contains a list of micronodes.
 */
public interface MicronodeGraphFieldList extends ListGraphField<MicronodeGraphField, MicronodeFieldList, Micronode> {

	String TYPE = "micronode";

	/**
	 * Create a new empty micronode and add it to the list.
	 * 
	 * @return
	 */
	Micronode createMicronode();

	/**
	 * Update the micronode list using the rest model list as a source.
	 * 
	 * @param ac
	 * @param list
	 * @return
	 */
	Single<Boolean> update(InternalActionContext ac, MicronodeFieldList list);
}
