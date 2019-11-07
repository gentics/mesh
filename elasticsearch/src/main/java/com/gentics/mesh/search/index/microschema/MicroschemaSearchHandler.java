package com.gentics.mesh.search.index.microschema;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class MicroschemaSearchHandler extends AbstractSearchHandler<MicroschemaContainer, MicroschemaResponse> {

	@Inject
	public MicroschemaSearchHandler(Database db, SearchProvider searchProvider, MicroschemaContainerIndexHandler indexHandler, MeshOptions options) {
		super(db, searchProvider, options, indexHandler);
	}

}
