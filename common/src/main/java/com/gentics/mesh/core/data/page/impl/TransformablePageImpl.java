package com.gentics.mesh.core.data.page.impl;

import com.gentics.mesh.core.data.TransformableElement;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * @see TransformablePage
 * @param <T>
 *            Type of the page element
 */
public class TransformablePageImpl<T extends TransformableElement<? extends RestModel>> extends PageImpl<T> implements TransformablePage<T> {

	/**
	 * Transform a page into a transformable page.
	 * 
	 * @param page
	 */
	public TransformablePageImpl(Page<T> page) {
		super(page.getWrappedList(), page.getTotalElements(), page.getNumber(), page.getPageCount(), page.getPerPage());
	}

}
