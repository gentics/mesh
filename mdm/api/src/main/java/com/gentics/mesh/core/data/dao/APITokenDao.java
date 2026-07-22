package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.user.HibAPITokenData;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserAPITokenDataModel;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for API Tokens
 */
public interface APITokenDao extends Dao<HibAPITokenData>, DaoTransformable<HibAPITokenData, UserAPITokenDataModel> {
	/**
	 * Create an API Token
	 * @param user user (owner)
	 * @param name name
	 * @param tokenId token ID
	 * @param expires optional expiry timestamp
	 * @return created instance
	 */
	HibAPITokenData create(HibUser user, String name, String tokenId, Long expires);

	/**
	 * Find an API Token for a user be UUID
	 * @param user user
	 * @param uuid UUID
	 * @return instance or null
	 */
	HibAPITokenData findByUuid(HibUser user, String uuid);

	/**
	 * Find an API Token for a user by Token ID
	 * @param user user
	 * @param tokenId Token ID
	 * @return instance or null
	 */
	HibAPITokenData findByTokenId(HibUser user, String tokenId);

	/**
	 * Find all API Tokens for a user
	 * @param ac action context
	 * @param user user
	 * @param pagingInfo optional paging info
	 * @return page of API Token instances
	 */
	Page<? extends HibAPITokenData> findAll(InternalActionContext ac, HibUser user, PagingParameters pagingInfo);
}
