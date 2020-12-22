package com.gentics.mesh.search.index.microschema;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.MicroschemaDAOActions;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

/**
 * Handler for microschema search index queries.
 */
@Singleton
public class MicroschemaSearchHandler extends AbstractSearchHandler<HibMicroschema, MicroschemaResponse> {

	@Inject
	public MicroschemaSearchHandler(Database db, SearchProvider searchProvider, MicroschemaContainerIndexHandlerImpl indexHandler, AbstractMeshOptions options,
		MicroschemaDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
