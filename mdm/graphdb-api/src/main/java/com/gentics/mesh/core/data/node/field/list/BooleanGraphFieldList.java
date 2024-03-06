package com.gentics.mesh.core.data.node.field.list;

import com.gentics.mesh.core.data.node.field.HibBooleanField;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;

/**
 * Domain model definition for a boolean list.
 */
public interface BooleanGraphFieldList extends ListGraphField<HibBooleanField, BooleanFieldListImpl, Boolean>, HibBooleanFieldList {

}
