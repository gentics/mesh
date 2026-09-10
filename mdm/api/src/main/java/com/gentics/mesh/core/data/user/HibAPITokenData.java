package com.gentics.mesh.core.data.user;

import java.util.Optional;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.HibNamedElement;
import com.gentics.mesh.core.rest.user.UserAPITokenDataModel;
import com.gentics.mesh.util.DateUtils;

/**
 * Domain model for API Token Data
 */
public interface HibAPITokenData extends HibCoreElement<UserAPITokenDataModel>, HibNamedElement {
	TypeInfo TYPE_INFO = new TypeInfo(ElementType.APITOKEN, null, null, null);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	@Override
	default String getSubETag(InternalActionContext ac) {
		return null;
	}

	/**
	 * Return the user to which the token belongs
	 * @return user
	 */
	HibUser getUser();

	/**
	 * Set the user
	 * @param user user
	 * @return fluent API
	 */
	HibAPITokenData setUser(HibUser user);

	/**
	 * Return the API token id
	 * @return token id
	 */
	String getTokenId();

	/**
	 * Set the user API token id.
	 *
	 * @param tokenId token id
	 * @return Fluent API
	 */
	HibAPITokenData setTokenId(String tokenId);

	/**
	 * Return the timestamp when the api key token code was issued.
	 *
	 * @return timestamp
	 */
	Long getIssuedTimestamp();

	/**
	 * Return the API token issue date.
	 *
	 * @return ISO8601 formatted date or null if the date has not yet been set
	 */
	default String getIssuedDate() {
		return Optional.ofNullable(getIssuedTimestamp()).map(DateUtils::toISO8601).orElse(null);
	}

	/**
	 * Set the API token issue timestamp to the current time.
	 *
	 * @return Fluent API
	 */
	HibAPITokenData setIssuedTimestamp();

	/**
	 * Return the timestamp when the api token was last used
	 * @return timestamp
	 */
	Long getLastUsedTimestamp();

	/**
	 * Return the date when the token was last used
	 * @return ISO8601 formatted date or null if the token was not yet used
	 */
	default String getLastUsedDate() {
		return Optional.ofNullable(getLastUsedTimestamp()).map(DateUtils::toISO8601).orElse(null);
	}

	/**
	 * Set the last used timestamp to the current time
	 * @return fluent API
	 */
	HibAPITokenData setLastUsedTimestamp();

	/**
	 * Return the timestamp when the token will expire (null if it does not expire)
	 * @return timestamp
	 */
	Long getExpiresTimestamp();

	/**
	 * Return the date when the token will expire
	 * @return ISO8601 formatted date or null if the token does not expire
	 */
	default String getExpiresDate() {
		return Optional.ofNullable(getExpiresTimestamp()).map(DateUtils::toISO8601).orElse(null);
	}

	/**
	 * Set the timestamp when the token will expired (null if the token shall not expire)
	 * @param expires timestamp
	 * @return fluent API
	 */
	HibAPITokenData setExpiresTimestamp(Long expires);

	/**
	 * Return whether the api token is valid (not expired)
	 * @return flag
	 */
	boolean isValid();

	@Override
	default String getAPIPath(InternalActionContext ac) {
		return null;
	}
}
