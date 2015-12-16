package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.handler.ActionContext;

import rx.Observable;

public interface MicronodeGraphFieldList extends ListGraphField<MicronodeGraphField, MicronodeFieldList> {

	public static final String TYPE = "micronode";

	Micronode createMicronode(MicronodeField field);

	Observable<Boolean> update(ActionContext ac, MicronodeFieldList list);
}
