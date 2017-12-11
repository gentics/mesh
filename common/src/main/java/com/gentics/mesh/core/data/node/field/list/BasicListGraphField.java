package com.gentics.mesh.core.data.node.field.list;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.node.field.nesting.ListableGraphField;
import com.gentics.mesh.core.rest.node.field.Field;

/**
 * 
 * @param <T>
 * @param <RM>
 * @param <U>
 *            Basic type which the list holds
 */
public interface BasicListGraphField<T extends ListableGraphField, RM extends Field, U> extends ListGraphField<T, RM, U> {

	void setList(List<U> list);

	default void setList(U... items) {
		setList(Arrays.asList(items));
	}
}
