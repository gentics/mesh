package com.gentics.mesh.search.index.tagfamily;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.graphdb.spi.LegacyDatabase;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class TagFamilySearchHandler extends AbstractSearchHandler<TagFamily, TagFamilyResponse> {

	@Inject
	public TagFamilySearchHandler(LegacyDatabase db, SearchProvider searchProvider, TagFamilyIndexHandler indexHandler) {
		super(db, searchProvider, indexHandler);
	}

}
