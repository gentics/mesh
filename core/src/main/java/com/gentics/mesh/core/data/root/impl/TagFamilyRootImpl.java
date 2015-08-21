package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getProject;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static com.gentics.mesh.util.VerticleHelper.triggerEvent;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily>implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	@Override
	protected Class<? extends TagFamily> getPersistanceClass() {
		return TagFamilyImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_TAG_FAMILY;
	}

	@Override
	public TagFamily create(String name, User creator) {

		TagFamilyImpl tagFamily = getGraph().addFramedVertex(TagFamilyImpl.class);
		tagFamily.setName(name);
		addTagFamily(tagFamily);
		tagFamily.setCreator(creator);
		tagFamily.setEditor(creator);
		// TODO set creation and editing timestamps
		TagFamilyRoot root = BootstrapInitializer.getBoot().tagFamilyRoot();
		if (root != null && !root.equals(this)) {
			root.addTagFamily(tagFamily);
		}
		return tagFamily;
	}

	@Override
	public void removeTagFamily(TagFamily tagFamily) {
		removeItem(tagFamily);
	}

	@Override
	public void addTagFamily(TagFamily tagFamily) {
		addItem(tagFamily);
	}

	@Override
	public void delete() {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamilyRoot {" + getUuid() + "}");
		}
		for (TagFamily tagFamily : findAll()) {
			tagFamily.delete();
		}
		getElement().remove();
	}

	@Override
	public void create(RoutingContext rc, Handler<AsyncResult<TagFamily>> handler) {
		I18NService i18n = I18NService.getI18n();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		try (Trx tx = new Trx(db)) {
			Project project = getProject(rc);
			MeshAuthUser requestUser = getUser(rc);
			TagFamilyCreateRequest requestModel = fromJson(rc, TagFamilyCreateRequest.class);

			String name = requestModel.getName();
			if (StringUtils.isEmpty(name)) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tagfamily_name_not_set"))));
			} else {
				if (project.getTagFamilyRoot().findByName(name) != null) {
					handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "tagfamily_conflicting_name", name))));
					return;
				}

				TagFamilyRoot root = project.getTagFamilyRoot();
				if (requestUser.hasPermission(root, CREATE_PERM)) {
					TagFamily tagFamily = null;
					try (Trx txCreate = new Trx(db)) {
						tagFamily = root.create(name, requestUser);
						root.addTagFamily(tagFamily);
						requestUser.addCRUDPermissionOnRole(root, CREATE_PERM, tagFamily);
						txCreate.success();
					}
					handler.handle(Future.succeededFuture(tagFamily));
				} else {
					handler.handle(Future.failedFuture(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", root.getUuid()))));
				}
			}
		}

	}

}
