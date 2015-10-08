package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.error.EntityNotFoundException;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

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
	public void create(InternalActionContext ac, Handler<AsyncResult<Tag>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.noTrx(noTx -> {
			Project project = ac.getProject();
			TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
			String tagName = requestModel.getFields().getName();
			if (StringUtils.isEmpty(tagName)) {
				handler.handle(failedFuture(ac, BAD_REQUEST, ac.i18n("tag_name_not_set")));
				return;
			}

			TagFamilyReference reference = requestModel.getTagFamilyReference();
			if (reference == null) {
				handler.handle(failedFuture(ac, BAD_REQUEST, ac.i18n("tag_tagfamily_reference_not_set")));
				return;
			}
			boolean hasName = !isEmpty(reference.getName());
			boolean hasUuid = !isEmpty(reference.getUuid());
			if (!hasUuid && !hasName) {
				handler.handle(failedFuture(ac, BAD_REQUEST, ac.i18n("tag_tagfamily_reference_uuid_or_name_missing")));
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
				throw new EntityNotFoundException(ac.i18n("tagfamily_not_found", nameOrUuid));
			}

			MeshAuthUser requestUser = ac.getUser();
			if (!requestUser.hasPermission(ac, tagFamily, CREATE_PERM)) {
				throw new InvalidPermissionException(ac.i18n("error_missing_perm", tagFamily.getUuid()));
			}

			if (tagFamily.findTagByName(tagName) != null) {
				handler.handle(failedFuture(ac, CONFLICT, "tag_create_tag_with_same_name_already_exists", tagName, tagFamily.getName()));
				return;
			}
			final TagFamily foundFamily = tagFamily;
			db.trx(tc -> {
				this.reload();
				requestUser.reload();
				// tagFamily.reload();
				project.reload();
				Tag newTag = foundFamily.create(requestModel.getFields().getName(), project, requestUser);
				ac.getUser().addCRUDPermissionOnRole(foundFamily, CREATE_PERM, newTag);
				ac.getUser().addCRUDPermissionOnRole(this, CREATE_PERM, newTag);
				BootstrapInitializer.getBoot().meshRoot().getTagRoot().addTag(newTag);
				project.getTagRoot().addTag(newTag);

				SearchQueueBatch batch = newTag.addIndexBatch(CREATE_ACTION);
				tc.complete(Tuple.tuple(batch, newTag));
			} , (AsyncResult<Tuple<SearchQueueBatch, Tag>> rh) -> {
				if (rh.failed()) {
					handler.handle(Future.failedFuture(rh.cause()));
				} else {
					processOrFail(ac, rh.result().v1(), handler, rh.result().v2());
				}
			});
		});
	}

}
