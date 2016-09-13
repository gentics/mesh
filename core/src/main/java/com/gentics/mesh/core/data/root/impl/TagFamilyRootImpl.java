package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.Stack;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.collect.Tuple;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Single;

public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily> implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void init(Database database) {
		database.addVertexType(TagFamilyRootImpl.class, MeshVertexImpl.class);
		database.addEdgeIndex(HAS_TAG_FAMILY, true, false, true);
	}

	@Override
	public Class<? extends TagFamily> getPersistanceClass() {
		return TagFamilyImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_TAG_FAMILY;
	}

	@Override
	public Project getProject() {
		Project project = in(HAS_TAGFAMILY_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
		return project;
	}

	@Override
	public TagFamily create(String name, User creator) {
		TagFamilyImpl tagFamily = getGraph().addFramedVertex(TagFamilyImpl.class);
		tagFamily.setName(name);
		addTagFamily(tagFamily);
		tagFamily.setCreated(creator);

		// Add tag family to project
		tagFamily.setProject(getProject());

		// Add created tag family to tag family root
		TagFamilyRoot root = MeshInternal.get().boot().tagFamilyRoot();
		if (root != null && !root.equals(this)) {
			root.addTagFamily(tagFamily);
		}

		// Add tag root to created tag family
		TagRoot tagRoot = getGraph().addFramedVertex(TagRootImpl.class);
		tagFamily.setTagRoot(tagRoot);

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
	public void delete(SearchQueueBatch batch) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamilyRoot {" + getUuid() + "}");
		}
		for (TagFamily tagFamily : findAll()) {
			tagFamily.delete(batch);
		}
		getElement().remove();
	}

	@Override
	public Single<TagFamily> create(InternalActionContext ac) {
		Database db = MeshInternal.get().database();

		return db.noTx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

			String name = requestModel.getName();
			if (StringUtils.isEmpty(name)) {
				throw error(BAD_REQUEST, "tagfamily_name_not_set");
			}

			// Check whether the name is already in-use.
			TagFamily conflictingTagFamily = findByName(name);
			if (conflictingTagFamily != null) {
				throw conflict(conflictingTagFamily.getUuid(), name, "tagfamily_conflicting_name", name);
			}

			if (requestUser.hasPermission(this, CREATE_PERM)) {
				Tuple<SearchQueueBatch, TagFamily> tuple = db.tx(() -> {
					requestUser.reload();
					this.reload();
					this.setElement(null);
					TagFamily tagFamily = create(name, requestUser);
					addTagFamily(tagFamily);
					requestUser.addCRUDPermissionOnRole(this, CREATE_PERM, tagFamily);
					SearchQueueBatch batch = tagFamily.createIndexBatch(STORE_ACTION);
					return Tuple.tuple(batch, tagFamily);
				});
				SearchQueueBatch batch = tuple.v1();
				TagFamily createdTagFamily = tuple.v2();

				return batch.process().toSingleDefault(createdTagFamily);
			} else {
				throw error(FORBIDDEN, "error_missing_perm", this.getUuid());
			}

		});

	}

	@Override
	public Single<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return Single.just(this);
		} else {
			String uuidSegment = stack.pop();
			TagFamily tagFamily = findByUuid(uuidSegment);
			if (stack.isEmpty()) {
				return Single.just(tagFamily);
			} else {
				String nestedRootNode = stack.pop();
				if ("tags".contentEquals(nestedRootNode)) {
					return tagFamily.getTagRoot().resolveToElement(stack);
				} else {
					return Single.error(new Exception("Unknown tagFamily element {" + nestedRootNode + "}"));
				}
			}
		}
	}

}
