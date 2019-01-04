package com.gentics.mesh.search.index.tag;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class TagSearchHandler extends AbstractSearchHandler<Tag, TagResponse> {

	@Inject
	public TagSearchHandler(LegacyDatabase db, SearchProvider searchProvider, TagIndexHandler indexHandler) {
		super(db, searchProvider, indexHandler);
	}

}
