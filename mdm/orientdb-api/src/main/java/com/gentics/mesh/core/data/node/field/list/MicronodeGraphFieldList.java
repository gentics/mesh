package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;

/**
 * Graph field which contains a list of micronodes.
 */
public interface MicronodeGraphFieldList extends HibMicronodeFieldList, ListGraphField<HibMicronodeField, MicronodeFieldList, HibMicronode> {

	String TYPE = "micronode";

}
