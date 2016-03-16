package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_FIELD_CONTAINER;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.TagImpl;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyCrudHandler;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class TagRootImpl extends AbstractRootVertex<Tag> implements TagRoot {

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_TAG);
		database.addVertexType(TagRootImpl.class);
	}

	private static final Logger log = LoggerFactory.getLogger(TagRootImpl.class);

	@Override
	public Class<? extends Tag> getPersistanceClass() {
		return TagImpl.class;
	}

	@Override
	public String getRootLabel() {
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
	public Observable<Tag> findByName(String name) {
		return Observable.just(out(getRootLabel()).has(getPersistanceClass()).mark().out(HAS_FIELD_CONTAINER).has("name", name).back()
				.nextOrDefaultExplicit(TagImpl.class, null));
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
	public Tag create(String name, Project project, TagFamily tagFamily, User creator) {
		TagImpl tag = getGraph().addFramedVertex(TagImpl.class);
		tag.setName(name);
		tag.setCreated(creator);
		tag.setProject(project);
		addTag(tag);

		// Add to global list of tags
		TagRoot globalTagRoot = BootstrapInitializer.getBoot().tagRoot();
		if (this != globalTagRoot) {
			globalTagRoot.addTag(tag);
		}

		// Add tag to project list of tags
		TagRoot projectTagRoot = project.getTagRoot();
		if (this != projectTagRoot) {
			projectTagRoot.addTag(tag);
		}

		// Set the tag family for the tag
		tag.setTagFamily(tagFamily);

		return tag;
	}

	@Override
	public Observable<Tag> create(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		return db.noTrx(() -> {
			Project project = ac.getProject();
			TagCreateRequest requestModel = ac.fromJson(TagCreateRequest.class);
			String tagName = requestModel.getFields().getName();
			if (isEmpty(tagName)) {
				throw error(BAD_REQUEST, "tag_name_not_set");
			}

//			TagFamilyReference reference = requestModel.getTagFamily();
//			if (reference == null) {
//				throw error(BAD_REQUEST, "tag_tagfamily_reference_not_set");
//			}
//			boolean hasName = !isEmpty(reference.getName());
//			boolean hasUuid = !isEmpty(reference.getUuid());
//			if (!hasUuid && !hasName) {
//				throw error(BAD_REQUEST, "tag_tagfamily_reference_uuid_or_name_missing");
//			}

			// First try the tag family reference by uuid if specified
//			TagFamily tagFamily = null;
//			String nameOrUuid = null;
//			if (hasUuid) {
//				nameOrUuid = reference.getUuid();
//				tagFamily = project.getTagFamilyRoot().findByUuid(reference.getUuid()).toBlocking().first();
//			} else if (hasName) {
//				nameOrUuid = reference.getName();
//				tagFamily = project.getTagFamilyRoot().findByName(reference.getName()).toBlocking().first();
//			}
			
			TagFamily tagFamily = ac.get(TagFamilyCrudHandler.TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY);
			if (tagFamily == null) {
				throw error(NOT_FOUND, "tagfamily_not_found");
			}

			MeshAuthUser requestUser = ac.getUser();
			if (!requestUser.hasPermissionSync(ac, tagFamily, CREATE_PERM)) {
				throw error(FORBIDDEN, "error_missing_perm", tagFamily.getUuid());
			}

			Tag conflictingTag = tagFamily.getTagRoot().findByName(tagName).toBlocking().single();
			if (conflictingTag != null) {
				throw conflict(conflictingTag.getUuid(), tagName, "tag_create_tag_with_same_name_already_exists", tagName, tagFamily.getName());
			}

			final TagFamily foundFamily = tagFamily;
			Tuple<SearchQueueBatch, Tag> tuple = db.trx(() -> {
				this.reload();
				requestUser.reload();
				project.reload();
				Tag newTag = foundFamily.create(requestModel.getFields().getName(), project, requestUser);
				ac.getUser().addCRUDPermissionOnRole(foundFamily, CREATE_PERM, newTag);
				ac.getUser().addCRUDPermissionOnRole(this, CREATE_PERM, newTag);
				BootstrapInitializer.getBoot().meshRoot().getTagRoot().addTag(newTag);
				foundFamily.getTagRoot().addTag(newTag);

				SearchQueueBatch batch = newTag.createIndexBatch(CREATE_ACTION);
				return Tuple.tuple(batch, newTag);
			});

			SearchQueueBatch batch = tuple.v1();
			Tag tag = tuple.v2();

			return batch.process().map(t -> tag);
		});
	}

}
