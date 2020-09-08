package com.gentics.mesh.search.index.tagfamily;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;

@Singleton
public class TagFamilySearchHandler extends AbstractSearchHandler<HibTagFamily, TagFamilyResponse> {

	@Inject
	public TagFamilySearchHandler(Database db, SearchProvider searchProvider, TagFamilyIndexHandlerImpl indexHandler, MeshOptions options, TagFamilyDAOActions actions) {
		super(db, searchProvider, options, indexHandler, actions);
	}

}
