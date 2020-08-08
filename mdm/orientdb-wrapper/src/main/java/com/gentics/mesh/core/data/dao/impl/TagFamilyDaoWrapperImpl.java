package com.gentics.mesh.core.data.dao.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.generic.PermissionProperties;
import com.gentics.mesh.core.data.root.TagFamilyRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;

import dagger.Lazy;

// TODO there is no tag family root since the tag itself is the root. 
public class TagFamilyDaoWrapperImpl implements TagFamilyDaoWrapper {

	private final Lazy<BootstrapInitializer> boot;
	private final Lazy<PermissionProperties> permissions;

	@Inject
	public TagFamilyDaoWrapperImpl(Lazy<BootstrapInitializer> boot, Lazy<PermissionProperties> permissions) {
		this.boot = boot;
		this.permissions = permissions;
	}

	@Override
	public TraversalResult<? extends TagFamily> findAllGlobal() {
		return boot.get().tagFamilyRoot().findAll();
	}

	@Override
	public TagFamily findByName(Project project, String name) {
		return project.getTagFamilyRoot().findByName(name);
	}

	@Override
	public TagFamily findByUuid(Project project, String uuid) {
		return project.getTagFamilyRoot().findByUuid(uuid);
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

	public void setRolePermissions(MeshVertex vertex, InternalActionContext ac, GenericRestResponse model) {
		model.setRolePerms(permissions.get().getRolePermissions(vertex, ac, ac.getRolePermissionParameters().getRoleUuid()));
	}

	@Override
	public boolean update(TagFamily tagFamily, InternalActionContext ac, EventQueueBatch batch) {
		TagFamilyUpdateRequest requestModel = ac.fromJson(TagFamilyUpdateRequest.class);
		Project project = ac.getProject();
		String newName = requestModel.getName();

		if (isEmpty(newName)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}

		TagFamily tagFamilyWithSameName = project.getTagFamilyRoot().findByName(newName);
		if (tagFamilyWithSameName != null && !tagFamilyWithSameName.getUuid().equals(tagFamily.getUuid())) {
			throw conflict(tagFamilyWithSameName.getUuid(), newName, "tagfamily_conflicting_name", newName);
		}
		if (!tagFamily.getName().equals(newName)) {
			tagFamily.setName(newName);
			batch.add(tagFamily.onUpdated());
			return true;
		}
		return false;
	}

	@Override
	public TagFamily create(Project project, InternalActionContext ac, EventQueueBatch batch, String uuid) {
		MeshAuthUser requestUser = ac.getUser();
		UserRoot userRoot = boot.get().userDao();
		TagFamilyCreateRequest requestModel = ac.fromJson(TagFamilyCreateRequest.class);

		String name = requestModel.getName();
		if (StringUtils.isEmpty(name)) {
			throw error(BAD_REQUEST, "tagfamily_name_not_set");
		}
		TagFamilyRoot projectTagFamilyRoot = project.getTagFamilyRoot();

		// Check whether the name is already in-use.
		TagFamily conflictingTagFamily = projectTagFamilyRoot.findByName(name);
		if (conflictingTagFamily != null) {
			throw conflict(conflictingTagFamily.getUuid(), name, "tagfamily_conflicting_name", name);
		}

		if (!userRoot.hasPermission(requestUser, projectTagFamilyRoot, CREATE_PERM)) {
			throw error(FORBIDDEN, "error_missing_perm", projectTagFamilyRoot.getUuid(), CREATE_PERM.getRestPerm().getName());
		}
		TagFamily tagFamily = projectTagFamilyRoot.create(name, requestUser, uuid);
		projectTagFamilyRoot.addTagFamily(tagFamily);
		userRoot.inheritRolePermissions(requestUser, projectTagFamilyRoot, tagFamily);

		batch.add(tagFamily.onCreated());
		return tagFamily;
	}

	@Override
	public TraversalResult<? extends TagFamily> findAll(Project project) {
		return project.getTagFamilyRoot().findAll();
	}

}
