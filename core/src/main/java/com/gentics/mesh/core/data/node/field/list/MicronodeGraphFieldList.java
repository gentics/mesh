package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.handler.ActionContext;

import rx.Observable;

public interface MicronodeGraphFieldList extends ListGraphField<MicronodeGraphField, MicronodeFieldList> {

	public static final String TYPE = "micronode";

	/**
	 * Create a new micronode using the rest model as a source.
	 * 
	 * @param field
	 * @return
	 */
	Micronode createMicronode(MicronodeField field);

	/**
	 * Update the micronode list using the rest model list as a source.
	 * 
	 * @param ac
	 * @param list
	 * @return
	 */
	Observable<Boolean> update(ActionContext ac, MicronodeFieldList list);
}
