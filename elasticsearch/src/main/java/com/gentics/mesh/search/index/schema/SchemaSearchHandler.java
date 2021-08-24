package com.gentics.mesh.search.index.schema;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.SchemaDAOActions;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

/**
 * Search handler for queries to the schema index.
 */
@Singleton
public class SchemaSearchHandler extends AbstractSearchHandler<HibSchema, SchemaResponse> {

	@Inject
	public SchemaSearchHandler(Database db, SearchProvider searchProvider, SchemaContainerIndexHandlerImpl indexHandler, MeshOptions options, SchemaDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
