package com.gentics.mesh.search.index.schema;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class SchemaSearchHandler extends AbstractSearchHandler<SchemaContainer, SchemaResponse> {

	@Inject
	public SchemaSearchHandler(LegacyDatabase db, SearchProvider searchProvider, SchemaContainerIndexHandler indexHandler) {
		super(db, searchProvider, indexHandler);
	}

}
