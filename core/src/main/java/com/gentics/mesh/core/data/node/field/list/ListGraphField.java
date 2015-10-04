package com.gentics.mesh.core.data.node.field.list;

import java.util.List;

import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface ListGraphField<T extends ListableGraphField, RM extends Field> extends MicroschemaListableGraphField, MeshVertex {

	/**
	 * Return the items of the list.
	 * 
	 * @return
	 */
	List<? extends T> getList();

	/**
	 * Return the current size of the list.
	 * 
	 * @return
	 */
	long getSize();

	/**
	 * Add the given item to the list.
	 * 
	 * @param item
	 */
	void addItem(T item);

	/**
	 * Remove the given item from the list.
	 * 
	 * @param item
	 */
	void removeItem(T item);

	/**
	 * Remove all items from the list.
	 */
	void removeAll();

	/**
	 * Return the list type.
	 * 
	 * @return
	 */
	Class<? extends T> getListType();

	/**
	 * Transform the list to the rest model.
	 * 
	 * @param ac
	 * @param fieldKey
	 * @param handler
	 */
	void transformToRest(InternalActionContext ac, String fieldKey, Handler<AsyncResult<RM>> handler);

}