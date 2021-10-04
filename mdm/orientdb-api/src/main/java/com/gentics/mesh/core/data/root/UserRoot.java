package com.gentics.mesh.core.data.root;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;

import java.util.function.Predicate;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicNonTransformablePageImpl;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation node for users.
 */
public interface UserRoot extends RootVertex<User>, TransformableElementRoot<User, UserResponse> {
	public static final String TYPE = "users";

	/**
	 * Find the user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	User findByUsername(String username);

	/**
	 * Add the user to the aggregation vertex.
	 * 
	 * @param user
	 */
	void addUser(User user);

	/**
	 * Remove the user from the aggregation vertex.
	 * 
	 * @param user
	 */
	void removeUser(User user);

	/**
	 * Dedicated find all method which fixes type issues with the predicate argument. The type of root vertex needs to be User instead of HibUser but the type
	 * of the predicate is inferred via the type of the root element. Thus we need to wrap the predicate.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	default Page<? extends HibUser> findAllWrapped(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibUser> extraFilter) {
		return new DynamicNonTransformablePageImpl<User>(ac.getUser(), this, pagingInfo, READ_PERM, user -> {
			return extraFilter.test(user);
		}, true);
	}

	/**
	 * Find the mesh auth user with the given username.
	 * 
	 * @param username
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUsername(String username);

	/**
	 * Find the mesh auth user with the given UUID.
	 * 
	 * @param userUuid
	 * @return
	 */
	MeshAuthUser findMeshAuthUserByUuid(String userUuid);

	/**
	 * Create a new user.
	 * 
	 * @return
	 */

	default User create() {
		return createRaw();
	}
}
