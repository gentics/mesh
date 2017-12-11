package com.gentics.mesh.core.data.node.field;

import com.gentics.mesh.core.data.node.field.list.BasicListGraphField;
import com.gentics.mesh.core.rest.node.field.ListField;
import com.gentics.mesh.core.rest.node.field.ListableField;
import com.syncleus.ferma.AbstractVertexFrame;

public abstract class AbstractBasicListField<T extends ListableField, RM, BT> extends AbstractBasicField<ListField<T>> implements BasicListGraphField<T, BT> {

	public AbstractBasicListField(String fieldKey, AbstractVertexFrame parentContainer) {
		super(fieldKey, parentContainer);
	}


}
