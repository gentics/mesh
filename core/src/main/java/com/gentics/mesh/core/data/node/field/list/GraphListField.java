package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.ActionContext;

public interface GraphListField<T extends ListableGraphField, RM extends Field> extends MicroschemaListableGraphField, MeshVertex {

	List<? extends T> getList();

	long getSize();

	void addItem(T item);

	void removeItem(T item);

	Class<? extends T> getListType();

	RM transformToRest(ActionContext ac, String fieldKey);


}