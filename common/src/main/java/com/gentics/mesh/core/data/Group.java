package com.gentics.mesh.core.data;

import static com.gentics.mesh.Events.EVENT_GROUP_CREATED;
import static com.gentics.mesh.Events.EVENT_GROUP_DELETED;
import static com.gentics.mesh.Events.EVENT_GROUP_UPDATED;
import static com.gentics.mesh.search.SearchProvider.INDEX_PREFIX;

import java.util.List;
import java.util.Objects;

import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Graph domain model interface for groups.
 */
public interface Group extends MeshCoreVertex<GroupResponse, Group>, ReferenceableElement<GroupReference>, UserTrackingVertex, IndexableElement {

	/**
	 * Type Value: {@value #TYPE}
	 */
	String TYPE = "group";

	TypeInfo TYPE_INFO = new TypeInfo(TYPE, EVENT_GROUP_CREATED, EVENT_GROUP_UPDATED, EVENT_GROUP_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Compose the index name for the group index.
	 * 
	 * @return
	 */
	static String composeIndexName() {
		return INDEX_PREFIX + Group.TYPE.toLowerCase();
	}

	/**
	 * Compose the document id for the group index.
	 * 
	 * @param groupUuid
	 * @return
	 */
	static String composeDocumentId(String groupUuid) {
		Objects.requireNonNull(groupUuid, "A groupUuid must be provided.");
		return groupUuid;
	}

	/**
	 * Assign the given user to this group.
	 * 
	 * @param user
	 */
	void addUser(User user);

	/**
	 * Unassign the user from the group.
	 * 
	 * @param user
	 */
	void removeUser(User user);

	/**
	 * Assign the given role to this group.
	 * 
	 * @param role
	 */
	void addRole(Role role);

	/**
	 * Unassign the role from this group.
	 * 
	 * @param role
	 */
	void removeRole(Role role);

	/**
	 * Return a list of users that are assigned to the group.
	 * 
	 * @return List of users
	 */
	List<? extends User> getUsers();

	/**
	 * Return the a list of roles that are assigned to the group.
	 * 
	 * @return List of roles
	 */
	List<? extends Role> getRoles();

	/**
	 * Check whether the user has been assigned to the group.
	 * 
	 * @param user
	 * @return
	 */
	boolean hasUser(User user);

	/**
	 * Check whether the role has been assigned to the group.
	 * 
	 * @param role
	 * @return
	 */
	boolean hasRole(Role role);

	/**
	 * Return a page with all visible roles that the given user can see.
	 * 
	 * @param User
	 *            user User which requested the resource
	 * @param pagingInfo
	 *            Paging information
	 * @return Page which contains the retrieved items
	 */
	TransformablePage<? extends Role> getRoles(User user, PagingParameters pagingInfo);

	/**
	 * Return a page with all users that the given user can see.
	 * 
	 * @param requestUser
	 * @param pagingInfo
	 * @return Page with found users, an empty page is returned when no users could be found
	 */
	TransformablePage<? extends User> getVisibleUsers(MeshAuthUser requestUser, PagingParameters pagingInfo);

}
