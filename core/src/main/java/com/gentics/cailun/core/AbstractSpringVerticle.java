package com.gentics.cailun.core;

import io.vertx.core.AbstractVerticle;

import org.neo4j.graphdb.GraphDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.neo4j.support.Neo4jTemplate;

import com.gentics.cailun.core.data.model.generic.GenericNode;
import com.gentics.cailun.core.data.service.CaiLunRootService;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.I18NService;
import com.gentics.cailun.core.data.service.LanguageService;
import com.gentics.cailun.core.data.service.ObjectSchemaService;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.data.service.RoleService;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.data.service.generic.GenericNodeService;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.etc.RouterStorage;

public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected CaiLunSpringConfiguration springConfiguration;

	@Autowired
	protected CaiLunRootService cailunRootService;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected GraphDatabaseService graphDb;

	@Autowired
	protected UserService userService;
	
	@Autowired
	protected LanguageService languageService;

	@Autowired
	protected RoleService roleService;

	@Autowired
	protected GenericNodeService<GenericNode> genericNodeService;

	@Autowired
	protected Neo4jTemplate neo4jTemplate;

	@Autowired
	protected ObjectSchemaService schemaService;

	@Autowired
	protected ProjectService projectService;

	@Autowired
	protected GroupService groupService;

	@Autowired
	protected ContentService contentService;
	
	@Autowired
	protected TagService tagService;

	public void setSpringConfig(CaiLunSpringConfiguration config) {
		this.springConfiguration = config;
	}

}
