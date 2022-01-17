package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.node.field.nesting.HibMicroschemaListableField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;

import io.reactivex.Single;

public interface HibMicronodeFieldList extends HibMicroschemaListableField, HibListField<HibMicronodeField, MicronodeFieldList, HibMicronode> {

	String TYPE = "micronode";

	/**
	 * Create a new empty micronode and add it to the list.
	 * 
	 * @return
	 */
	HibMicronode createMicronode();

	/**
	 * Update the micronode list using the rest model list as a source.
	 * 
	 * @param ac
	 * @param list
	 * @return
	 */
	Single<Boolean> update(InternalActionContext ac, MicronodeFieldList list);
}
