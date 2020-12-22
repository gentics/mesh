package com.gentics.mesh.search.index.tag;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

/**
 * Handler for tag index search operations.
 */
@Singleton
public class TagSearchHandler extends AbstractSearchHandler<HibTag, TagResponse> {

	@Inject
	public TagSearchHandler(Database db, SearchProvider searchProvider, TagIndexHandlerImpl indexHandler, AbstractMeshOptions options, TagDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
