package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.parameter.PagingParameters;

public interface OrientDBMicroschemaDao extends MicroschemaDao {

	/**
	 * Load a page of microschemas.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends Microschema> findAll(InternalActionContext ac, PagingParameters pagingInfo,
		Predicate<Microschema> extraFilter);
}
