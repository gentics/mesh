package com.gentics.mesh.search;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.gentics.mesh.search.index.GroupIndexHandler;
import com.gentics.mesh.search.index.MicroschemaContainerIndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;
import com.gentics.mesh.search.index.ProjectIndexHandler;
import com.gentics.mesh.search.index.RoleIndexHandler;
import com.gentics.mesh.search.index.SchemaContainerIndexHandler;
import com.gentics.mesh.search.index.TagFamilyIndexHandler;
import com.gentics.mesh.search.index.TagIndexHandler;
import com.gentics.mesh.search.index.UserIndexHandler;

@Component
public class SearchHelper {

	@Autowired
	private SearchProvider searchProvider;

	@Autowired
	private UserIndexHandler userIndexHandler;

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@Autowired
	private RoleIndexHandler roleIndexHandler;

	@Autowired
	private GroupIndexHandler groupIndexHandler;

	@Autowired
	private TagIndexHandler tagIndexHandler;

	@Autowired
	private ProjectIndexHandler projectIndexHandler;

	@Autowired
	private TagFamilyIndexHandler tagFamilyIndexHandler;

	@Autowired
	private MicroschemaContainerIndexHandler microschemaIndexHandler;

	@Autowired
	private SchemaContainerIndexHandler schemaIndexHandler;

	public void init() {

		userIndexHandler.init().toBlocking().last();
		groupIndexHandler.init().toBlocking().last();
		roleIndexHandler.init().toBlocking().last();

		projectIndexHandler.init().toBlocking().last();
		nodeIndexHandler.init().toBlocking().last();
		tagIndexHandler.init().toBlocking().last();
		tagFamilyIndexHandler.init().toBlocking().last();

		schemaIndexHandler.init().toBlocking().last();
		microschemaIndexHandler.init().toBlocking().last();

	}
}
