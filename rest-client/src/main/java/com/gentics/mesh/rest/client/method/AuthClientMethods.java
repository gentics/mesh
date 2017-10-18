package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Single;

public interface AuthClientMethods {

	/**
	 * Login the user using the credentials that have been set using {@link MeshRestClient#setLogin(String, String)}.
	 * 
	 * @return
	 */
	Single<GenericMessageResponse> login();

	/**
	 * Logout the user.
	 * 
	 * @return
	 */
	Single<GenericMessageResponse> logout();

	/**
	 * Return the currently active user's rest model data.
	 * 
	 * @return
	 */
	MeshRequest<UserResponse> me();

	/**
	 * Assign permissions in between the given role and the object uuid.
	 * 
	 * @param roleUuid
	 *            Role uuid that is used to assign permission to
	 * @param objectUuid
	 *            Uuid of the object to which permissions are granted or revoked
	 * @param permission
	 *            Permissions to be assigned. Omitted permissions will be revoked
	 * @param recursive
	 *            Define whether nested elements or child element should also be affected
	 * @return
	 */
	MeshRequest<GenericMessageResponse> permissions(String roleUuid, String objectUuid, Permission permission, boolean recursive);

}
