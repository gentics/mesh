package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.ActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
	public void create(ActionContext ac, Handler<AsyncResult<TagFamily>> handler) {
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();

		try (Trx tx = db.trx()) {
			MeshAuthUser requestUser = ac.getUser();
			TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

			String name = requestModel.getName();
			if (StringUtils.isEmpty(name)) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, ac.i18n("tagfamily_name_not_set"))));
			} else {
				if (findByName(name) != null) {
					handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT, ac.i18n("tagfamily_conflicting_name", name))));
					return;
				}

				if (requestUser.hasPermission(this, CREATE_PERM)) {
					TagFamily tagFamily = null;
					try (Trx txCreate = db.trx()) {
						requestUser.reload();
						tagFamily = create(name, requestUser);
						addTagFamily(tagFamily);
						requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, tagFamily);
						txCreate.success();
					}
					handler.handle(Future.succeededFuture(tagFamily));
				} else {
					handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", this.getUuid()))));
				}
			}
		}

	}

}
