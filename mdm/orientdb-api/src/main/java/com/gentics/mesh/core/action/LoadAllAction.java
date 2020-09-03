package com.gentics.mesh.core.action;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Loading schemas from a project requires a different loading action. This interface allows for different implementations.
 *
 * @see DAOActions
 * @param <T>
 */
public interface LoadAllAction<T extends HibBaseElement> {
	TransformablePage<? extends T> loadAll(DAOActionContext ctx, PagingParameters pagingInfo);
}
