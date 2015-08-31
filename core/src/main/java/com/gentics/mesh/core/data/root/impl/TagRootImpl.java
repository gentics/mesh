package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.VerticleHelper.getProject;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.util.TraversalHelper;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class TagRootImpl extends AbstractRootVertex<Tag>implements TagRoot {

	private static final Logger log = LoggerFactory.getLogger(TagRootImpl.class);

	@Override
	protected Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	protected String getRootLabel() {
		return HAS_TAG;
	}

	@Override
	public void addTag(Tag tag) {
		addItem(tag);
	}

	@Override
	public void removeTag(Tag tag) {
		removeItem(tag);
	}

	@Override
	public Tag findByName(String name) {
		return out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back()
				.nextOrDefaultExplicit(TagImpl.class, null);
	}

	@Override
	public void delete() {
		// TODO add check to prevent deletion of MeshRoot.tagRoot
		if (log.isDebugEnabled()) {
			log.debug("Deleting tag root {" + getUuid() + "}");
		}
		for (Tag tag : findAll()) {
			tag.delete();
		}
		getElement().remove();
	}

	@Override
	public void create(RoutingContext rc, Handler<AsyncResult<Tag>> handler) {
		I18NService i18n = I18NService.getI18n();
		Database db = MeshSpringConfiguration.getMeshSpringConfiguration().database();
		try (Trx tx = db.trx()) {

			Project project = getProject(rc);
			TagCreateRequest requestModel = fromJson(rc, TagCreateRequest.class);
			String tagName = requestModel.getFields().getName();
			if (StringUtils.isEmpty(tagName)) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tag_name_not_set"))));
				return;
			}

			TagFamilyReference reference = requestModel.getTagFamilyReference();
			if (reference == null) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tag_tagfamily_reference_not_set"))));
				return;
			}
			boolean hasName = !isEmpty(reference.getName());
			boolean hasUuid = !isEmpty(reference.getUuid());
			if (!hasUuid && !hasName) {
				handler.handle(Future
						.failedFuture(new HttpStatusCodeErrorException(BAD_REQUEST, i18n.get(rc, "tag_tagfamily_reference_uuid_or_name_missing"))));
				return;
			}

			// First try the tag family reference by uuid if specified
			TagFamily tagFamily = null;
			String nameOrUuid = null;
			if (hasUuid) {
				nameOrUuid = reference.getUuid();
				tagFamily = project.getTagFamilyRoot().findByUuidBlocking(reference.getUuid());
			} else if (hasName) {
				nameOrUuid = reference.getName();
				tagFamily = project.getTagFamilyRoot().findByName(reference.getName());
			}
			if (tagFamily == null) {
				throw new EntityNotFoundException(i18n.get(rc, "tagfamily_not_found", nameOrUuid));
			}

			MeshAuthUser requestUser = getUser(rc);
			if (!requestUser.hasPermission(tagFamily, CREATE_PERM)) {
				throw new InvalidPermissionException(i18n.get(rc, "error_missing_perm", tagFamily.getUuid()));
			}

			if (tagFamily.findTagByName(tagName) != null) {
				handler.handle(Future.failedFuture(new HttpStatusCodeErrorException(CONFLICT,
						i18n.get(rc, "tag_create_tag_with_same_name_already_exists", tagName, tagFamily.getName()))));
				return;
			}
			Tag newTag;
			SearchQueueBatch batch;
			try (Trx txCreate = db.trx()) {
				requestUser.reload();
				tagFamily.reload();
				project.reload();

				newTag = tagFamily.create(requestModel.getFields().getName(), project, requestUser);
				getUser(rc).addCRUDPermissionOnRole(this, CREATE_PERM, newTag);
				BootstrapInitializer.getBoot().meshRoot().getTagRoot().addTag(newTag);
				project.getTagRoot().addTag(newTag);

				batch = newTag.addIndexBatch(CREATE_ACTION);
				txCreate.success();
			}
			batch.process(rh -> {
				if (rh.succeeded()) {
					handler.handle(Future.succeededFuture(newTag));
				} else if (rh.failed()) {
					log.error("Error while processing batch for newly created tag {" + newTag.getUuid() + "}", rh.cause());
				}
			});

		}

	}

}
