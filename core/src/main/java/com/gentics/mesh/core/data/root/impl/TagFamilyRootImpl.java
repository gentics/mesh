package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.rest.error.HttpConflictErrorException.conflict;
import static com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException.failedFuture;
import static com.gentics.mesh.util.VerticleHelper.processOrFail;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily>implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_TAG_FAMILY);
		database.addVertexType(TagFamilyRootImpl.class);
	}

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
		tagFamily.setCreated(creator);
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
	public void create(InternalActionContext ac, Handler<AsyncResult<TagFamily>> handler) {
		Database db = MeshSpringConfiguration.getInstance().database();
		db.noTrx(noTx -> {
			MeshAuthUser requestUser = ac.getUser();
			TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

			String name = requestModel.getName();
			if (StringUtils.isEmpty(name)) {
				handler.handle(failedFuture(ac, BAD_REQUEST, ac.i18n("tagfamily_name_not_set")));
			} else {

				// Check whether the name is already in-use.
				TagFamily conflictingTagFamily = findByName(name);
				if (conflictingTagFamily != null) {
					HttpStatusCodeErrorException conflictError = conflict(ac, conflictingTagFamily.getUuid(), name, "tagfamily_conflicting_name",
							name);
					handler.handle(Future.failedFuture(conflictError));
					return;
				}
				if (requestUser.hasPermission(ac, this, CREATE_PERM)) {
					db.trx(txCreate -> {
						requestUser.reload();
						this.reload();
						this.setElement(null);
						TagFamily tagFamily = create(name, requestUser);
						addTagFamily(tagFamily);
						requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, tagFamily);
						SearchQueueBatch batch = tagFamily.addIndexBatch(SearchQueueEntryAction.CREATE_ACTION);
						txCreate.complete(Tuple.tuple(batch, tagFamily));
					} , (AsyncResult<Tuple<SearchQueueBatch, TagFamily>> txCreated) -> {
						if (txCreated.failed()) {
							handler.handle(Future.failedFuture(txCreated.cause()));
						} else {
							processOrFail(ac, txCreated.result().v1(), handler, txCreated.result().v2());
						}
					});
				} else {
					handler.handle(Future.failedFuture(new InvalidPermissionException(ac.i18n("error_missing_perm", this.getUuid()))));
				}
			}
		});

	}

}
