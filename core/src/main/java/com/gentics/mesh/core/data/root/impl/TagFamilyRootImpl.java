package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAGFAMILY_ROOT;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_TAG_FAMILY;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;

import java.util.Stack;

import org.apache.commons.lang3.StringUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
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
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see TagFamilyRoot
 */
public class TagFamilyRootImpl extends AbstractRootVertex<TagFamily> implements TagFamilyRoot {

	private static final Logger log = LoggerFactory.getLogger(ProjectImpl.class);

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(TagFamilyRootImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(HAS_TAG_FAMILY).withInOut().withOut());
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
	public TagFamily create(String name, User creator, String uuid) {
		TagFamilyImpl tagFamily = getGraph().addFramedVertex(TagFamilyImpl.class);
		if (uuid != null) {
			tagFamily.setUuid(uuid);
		}
		tagFamily.setName(name);
		addTagFamily(tagFamily);
		tagFamily.setCreated(creator);

		// Add tag family to project
		tagFamily.setProject(getProject());

		// Add created tag family to tag family root
		TagFamilyRoot root = mesh().boot().tagFamilyRoot();
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
	public void delete(BulkActionContext bac) {
		if (log.isDebugEnabled()) {
			log.debug("Deleting tagFamilyRoot {" + getUuid() + "}");
		}
		for (TagFamily tagFamily : findAll()) {
			tagFamily.delete(bac);
			bac.process();
		}
		getElement().remove();
		bac.inc();
		bac.process();
	}

	@Override
	public TagFamily create(InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		UserRoot userRoot = mesh().boot().userRoot();
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

		if (!userRoot.hasPermission(requestUser, this, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", this.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		TagFamily tagFamily = create(name, requestUser, uuid);
		addTagFamily(tagFamily);
		userRoot.inheritRolePermissions(requestUser, this, tagFamily);

		batch.add(tagFamily.onCreated());
		return tagFamily;
	}

	@Override
	public MeshVertex resolveToElement(Stack<String> stack) {
		if (stack.isEmpty()) {
			return this;
		} else {
			String uuidSegment = stack.pop();
			TagFamily tagFamily = findByUuid(uuidSegment);
			if (stack.isEmpty()) {
				return tagFamily;
			} else {
				String nestedRootNode = stack.pop();
				if ("tags".contentEquals(nestedRootNode)) {
					return tagFamily.resolveToElement(stack);
				} else {
					// TODO i18n
					throw error(NOT_FOUND, "Unknown tagFamily element {" + nestedRootNode + "}");
				}
			}
		}
	}

	@Override
	public TagFamilyResponse transformToRestSync(TagFamily tagFamily, InternalActionContext ac, int level, String... languageTags) {
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();

		TagFamilyResponse restTagFamily = new TagFamilyResponse();
		if (fields.has("uuid")) {
			restTagFamily.setUuid(tagFamily.getUuid());

			// Performance shortcut to return now and ignore the other checks
			if (fields.size() == 1) {
				return restTagFamily;
			}
		}

		if (fields.has("name")) {
			restTagFamily.setName(tagFamily.getName());
		}

		tagFamily.fillCommonRestFields(ac, fields, restTagFamily);

		if (fields.has("perms")) {
			setRolePermissions(tagFamily, ac, restTagFamily);
		}

		return restTagFamily;

	}

}
