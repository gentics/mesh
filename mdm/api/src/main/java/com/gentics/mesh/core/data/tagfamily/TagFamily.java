package com.gentics.mesh.core.data.tagfamily;

import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_UPDATED;
import static com.gentics.mesh.util.URIUtils.encodeSegment;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.BucketableElement;
import com.gentics.mesh.core.data.CoreElement;
import com.gentics.mesh.core.data.NamedBaseElement;
import com.gentics.mesh.core.data.ProjectElement;
import com.gentics.mesh.core.data.ReferenceableElement;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.role.Role;
import com.gentics.mesh.core.data.tag.Tag;
import com.gentics.mesh.core.data.user.UserTracking;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.core.rest.event.tagfamily.TagFamilyMeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectReference;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionUtils;

/**
 * Domain model for tag families.
 */
public interface TagFamily extends CoreElement<TagFamilyResponse>, ReferenceableElement<TagFamilyReference>, 
		ProjectElement, UserTracking, BucketableElement, NamedBaseElement {

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.TAGFAMILY, TAG_FAMILY_CREATED, TAG_FAMILY_UPDATED, TAG_FAMILY_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Set the project in which the tag family is used
	 * @param project
	 */
	void setProject(Project project);

	@Override
	default PermissionChangedProjectElementEventModel onPermissionChanged(Role role) {
		PermissionChangedProjectElementEventModel model = new PermissionChangedProjectElementEventModel();
		fillPermissionChanged(model, role);
		return model;
	}

	@Override
	default TagFamilyReference transformToReference() {
		return new TagFamilyReference().setName(getName()).setUuid(getUuid());
	}

	@Override
	default TagFamilyMeshEventModel createEvent(MeshEvent type) {
		TagFamilyMeshEventModel event = new TagFamilyMeshEventModel();
		event.setEvent(type);
		fillEventInfo(event);

		// .project
		Project project = getProject();
		ProjectReference reference = project.transformToReference();
		event.setProject(reference);

		return event;
	}

	/**
	 * Return the description.
	 * 
	 * @return
	 */
	String getDescription();

	/**
	 * Set the description.
	 * 
	 * @param description
	 */
	void setDescription(String description);

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return VersionUtils.baseRoute(ac) + "/" + encodeSegment(getProject().getName()) + "/tagFamilies/" + getUuid();
	}

	/**
	 * Construct the index name for tag family indices. Use the projectUuid in order to create a project specific index.
	 * 
	 * @param projectUuid
	 * @return
	 */
	static String composeIndexName(String projectUuid) {
		Objects.requireNonNull(projectUuid, "A projectUuid must be provided.");
		StringBuilder indexName = new StringBuilder();
		indexName.append("tagfamily");
		indexName.append("-").append(projectUuid);
		return indexName.toString();
	}

	/**
	 * Construct the documentId for tag family index documents.
	 * 
	 * @param elementUuid
	 * @return documentId
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

	@Override
	default boolean applyPermissions(MeshAuthUser authUser, EventQueueBatch batch, Role role, boolean recursive, Set<InternalPermission> permissionsToGrant,
                                     Set<InternalPermission> permissionsToRevoke) {
		UserDao userDao = Tx.get().userDao();
		boolean permissionChanged = false;
		if (recursive) {
			for (Tag tag : findAllTags().stream().filter(e -> userDao.hasPermission(authUser.getDelegate(), this, InternalPermission.READ_PERM)).collect(Collectors.toList())) {
				permissionChanged = tag.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
			}
		}
		permissionChanged = CoreElement.super.applyPermissions(authUser, batch, role, recursive, permissionsToGrant, permissionsToRevoke) || permissionChanged;
		return permissionChanged;
	}

	/**
	 * Add tag to the tag family
	 * @param tag
	 */
	void addTag(Tag tag);

	/**
	 * Remove tag from the tag family
	 * @param tag
	 */
	void removeTag(Tag tag);

	/**
	 * Find all tags of this tag family
	 * @return
	 */
	Result<? extends Tag> findAllTags();
}
