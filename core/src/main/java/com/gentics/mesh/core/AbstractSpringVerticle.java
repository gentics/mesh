package com.gentics.mesh.core;

import io.vertx.core.AbstractVerticle;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.I18NPropertyService;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.data.service.LanguageService;
import com.gentics.mesh.core.data.service.MeshNodeService;
import com.gentics.mesh.core.data.service.MeshRootService;
import com.gentics.mesh.core.data.service.ObjectSchemaService;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.data.service.RoleService;
import com.gentics.mesh.core.data.service.RoutingContextService;
import com.gentics.mesh.core.data.service.TagService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.etc.RouterStorage;
import com.tinkerpop.blueprints.TransactionalGraph;
import com.tinkerpop.frames.FramedGraph;

public abstract class AbstractSpringVerticle extends AbstractVerticle {

	public abstract void start() throws Exception;

	@Autowired
	protected FramedGraph<? extends TransactionalGraph> framedGraph;

	@Autowired
	protected MeshSpringConfiguration springConfiguration;

	@Autowired
	protected MeshRootService meshRootService;

	@Autowired
	protected RouterStorage routerStorage;

	@Autowired
	protected RoutingContextService rcs;

	@Autowired
	protected I18NService i18n;

	@Autowired
	protected UserService userService;

	@Autowired
	protected LanguageService languageService;

	@Autowired
	protected RoleService roleService;

	@Autowired
	protected ObjectSchemaService schemaService;

	@Autowired
	protected ProjectService projectService;

	@Autowired
	protected GroupService groupService;

	@Autowired
	protected MeshNodeService nodeService;

	@Autowired
	protected TagService tagService;

	@Autowired
	protected I18NPropertyService i18nPropertyService;

	public void setSpringConfig(MeshSpringConfiguration config) {
		this.springConfiguration = config;
	}

	public RoutingContextService getRcs() {
		return rcs;
	}

}
