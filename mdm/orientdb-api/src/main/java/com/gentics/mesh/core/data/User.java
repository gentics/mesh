package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;

import java.util.Objects;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.search.GraphDBBucketableElement;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;

/**
 * The User Domain Model interface.
 *
 * <pre>
* {@code
* 	(u:UserImpl)-[r1:HAS_USER]->(ur:UserRootImpl)
* 	(u)-[r2:HAS_USER]->(g:GroupImpl)
 	(g)<-[r3:HAS_ROLE]-(r:RoleImpl)
* }
 * </pre>
 *
 * <p>
 * <img src= "http://getmesh.io/docs/javadoc/cypher/com.gentics.mesh.core.data.impl.UserImpl.jpg" alt="">
 * </p>
 */
public interface User extends MeshCoreVertex<UserResponse>, ReferenceableElement<UserReference>, UserTrackingVertex, HibUser, GraphDBBucketableElement {

	/**
	 * API token id property name {@value #API_TOKEN_ID}
	 */
	String API_TOKEN_ID = "APITokenId";

	/**
	 * API token timestamp property name {@value #API_TOKEN_ISSUE_TIMESTAMP}
	 */
	String API_TOKEN_ISSUE_TIMESTAMP = "APITokenTimestamp";

	TypeInfo TYPE_INFO = new TypeInfo(ElementType.USER, USER_CREATED, USER_UPDATED, USER_DELETED);

	@Override
	default TypeInfo getTypeInfo() {
		return TYPE_INFO;
	}

	/**
	 * Compose the index name for the user index.
	 *
	 * @return
	 */
	static String composeIndexName() {
		return "user";
	}

	/**
	 * Compose the document id for the user documents.
	 *
	 * @param elementUuid
	 * @return
	 */
	static String composeDocumentId(String elementUuid) {
		Objects.requireNonNull(elementUuid, "A elementUuid must be provided.");
		return elementUuid;
	}

	/**
	 * Delete the element.
	 */
	@Deprecated
	void remove();

	/**
	 * Return the timestamp when the api key token code was last issued.
	 *
	 * @return
	 */
	default Long getAPITokenIssueTimestamp() {
		return property(API_TOKEN_ISSUE_TIMESTAMP);
	}

	/**
	 * Reset the API token id and issue timestamp and thus invalidating the token.
	 */
	default void resetAPIToken() {
		setProperty(API_TOKEN_ID, null);
		setProperty(API_TOKEN_ISSUE_TIMESTAMP, null);
	}
}
