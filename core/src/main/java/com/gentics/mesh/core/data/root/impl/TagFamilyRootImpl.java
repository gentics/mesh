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

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.TagFamilyImpl;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.TagRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import rx.Observable;

public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily> implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void checkIndices(Database database) {
		database.addEdgeIndex(HAS_TAG_FAMILY);
		database.addVertexType(TagFamilyRootImpl.class);
	}

	@Override
	public

	Class<? extends TagFamily> getPersistanceClass() {
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
		TagFamilyRoot root = BootstrapInitializer.getBoot().tagFamilyRoot();
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
	public Observable<TagFamily> create(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		return db.noTrx(() -> {
			MeshAuthUser requestUser = ac.getUser();
			TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

			String name = requestModel.getName();
			if (StringUtils.isEmpty(name)) {
				throw error(BAD_REQUEST, "tagfamily_name_not_set");
			}

			// Check whether the name is already in-use.
			TagFamily conflictingTagFamily = findByName(name).toBlocking().single();
			if (conflictingTagFamily != null) {
				throw conflict(conflictingTagFamily.getUuid(), name, "tagfamily_conflicting_name", name);
			}

			if (requestUser.hasPermissionSync(ac, this, CREATE_PERM)) {
				Tuple<SearchQueueBatch, TagFamily> tuple = db.trx(() -> {
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

				return batch.process().map(done -> createdTagFamily);
			} else {
				throw error(FORBIDDEN, "error_missing_perm", this.getUuid());
			}

		});

	}

	@Override
	public Observable<? extends MeshVertex> resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return Observable.just(this);
		} else {
			String uuidSegment = stack.pop();
			return findByUuid(uuidSegment).flatMap(tagFamily -> {
				if (stack.isEmpty()) {
					return Observable.just(tagFamily);
				} else {
					String nestedRootNode = stack.pop();
					if ("tags".contentEquals(nestedRootNode)) {
						return tagFamily.getTagRoot().resolveToElement(stack);
					} else {
						return Observable.error(new Exception("Unknown tagFamily element {" + nestedRootNode + "}"));
					}
				}
			});
		}
	}

}
