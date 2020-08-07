package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.ASSIGNED_TO_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE_REFERENCE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_ROLE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_USER;
import static com.gentics.mesh.madl.index.EdgeIndexDefinition.edgeIndex;

import java.util.Optional;
import java.util.Spliterator;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.apache.commons.lang3.BooleanUtils;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.DynamicTransformablePageImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.handler.VersionHandler;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.util.ETag;
import com.syncleus.ferma.traversals.VertexTraversal;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * @see User
 */
public class UserImpl extends AbstractMeshCoreVertex<UserResponse, User> implements User {

	private static final Logger log = LoggerFactory.getLogger(UserImpl.class);

	public static final String FIRSTNAME_PROPERTY_KEY = "firstname";

	public static final String LASTNAME_PROPERTY_KEY = "lastname";

	public static final String USERNAME_PROPERTY_KEY = "username";

	public static final String EMAIL_PROPERTY_KEY = "emailAddress";

	public static final String PASSWORD_HASH_PROPERTY_KEY = "passwordHash";

	public static final String ENABLED_FLAG_PROPERTY_KEY = "enabledFlag";

	public static final String ADMIN_FLAG_PROPERTY_KEY = "adminFlag";

	public static final String RESET_TOKEN_KEY = "resetToken";

	public static final String RESET_TOKEN_ISSUE_TIMESTAMP_KEY = "resetTokenTimestamp";

	public static final String FORCE_PASSWORD_CHANGE_KEY = "forcePasswordChange";

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(UserImpl.class, MeshVertexImpl.class);
		index.createIndex(edgeIndex(ASSIGNED_TO_ROLE).withOut());
	}

	@Override
	public User disable() {
		// TODO Fixme - The #delete method will currently remove the user instead of disabling it.
		// Thus this method is not used.
		property(ENABLED_FLAG_PROPERTY_KEY, false);
		return this;
	}

	@Override
	public Long getResetTokenIssueTimestamp() {
		return property(RESET_TOKEN_ISSUE_TIMESTAMP_KEY);
	}

	@Override
	public User setResetTokenIssueTimestamp(Long timestamp) {
		property(RESET_TOKEN_ISSUE_TIMESTAMP_KEY, timestamp);
		return this;
	}

	@Override
	public User setResetToken(String token) {
		property(RESET_TOKEN_KEY, token);
		return this;
	}

	@Override
	public String getResetToken() {
		return property(RESET_TOKEN_KEY);
	}

	@Override
	public boolean isForcedPasswordChange() {
		return Optional.<Boolean>ofNullable(property(FORCE_PASSWORD_CHANGE_KEY)).orElse(false);
	}

	@Override
	public User setForcedPasswordChange(boolean force) {
		property(FORCE_PASSWORD_CHANGE_KEY, force);
		return this;
	}

	@Override
	public User enable() {
		property(ENABLED_FLAG_PROPERTY_KEY, true);
		return this;
	}

	@Override
	public boolean isEnabled() {
		// TODO the #delete method will currently delete the user. It will not be deleted.
		// Boolean isEnabled = USER_STATE_CACHE.get(getUuid());
		// if (isEnabled == null) {
		// isEnabled = BooleanUtils.toBoolean(property(ENABLED_FLAG_PROPERTY_KEY).toString());
		// USER_STATE_CACHE.put(getUuid(), isEnabled);
		// }
		//
		// return isEnabled;
		return BooleanUtils.toBoolean(property(ENABLED_FLAG_PROPERTY_KEY).toString());
	}

	@Override
	public boolean isAdmin() {
		Boolean flag = property(ADMIN_FLAG_PROPERTY_KEY);
		return BooleanUtils.toBoolean(flag);
	}

	@Override
	public void setAdmin(boolean flag) {
		property(ADMIN_FLAG_PROPERTY_KEY, flag);
	}

	@Override
	public String getFirstname() {
		return property(FIRSTNAME_PROPERTY_KEY);
	}

	@Override
	public User setFirstname(String name) {
		property(FIRSTNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getLastname() {
		return property(LASTNAME_PROPERTY_KEY);
	}

	@Override
	public User setLastname(String name) {
		property(LASTNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getName() {
		return getUsername();
	}

	@Override
	public void setName(String name) {
		setUsername(name);
	}

	@Override
	public String getUsername() {
		return property(USERNAME_PROPERTY_KEY);
	}

	@Override
	public User setUsername(String name) {
		property(USERNAME_PROPERTY_KEY, name);
		return this;
	}

	@Override
	public String getEmailAddress() {
		return property(EMAIL_PROPERTY_KEY);
	}

	@Override
	public User setEmailAddress(String emailAddress) {
		property(EMAIL_PROPERTY_KEY, emailAddress);
		return this;
	}

	@Override
	public Page<? extends Group> getGroups(User user, PagingParameters params) {
		VertexTraversal<?, ?, ?> traversal = out(HAS_USER);
		return new DynamicTransformablePageImpl<Group>(user, traversal, params, READ_PERM, GroupImpl.class);
	}

	@Override
	public TraversalResult<? extends Group> getGroups() {
		return out(HAS_USER, GroupImpl.class);
	}

	@Override
	public String getRolesHash() {
		String indexName = "e." + ASSIGNED_TO_ROLE + "_out";
		Spliterator<Edge> itemEdges = getGraph().getEdges(indexName.toLowerCase(), id()).spliterator();
		String roles = StreamSupport.stream(itemEdges, false)
			.map(itemEdge -> itemEdge.getVertex(Direction.IN).getId().toString())
			.sorted()
			.collect(Collectors.joining());

		return ETag.hash(roles + String.valueOf(isAdmin()));
	}

	@Override
	public TraversalResult<? extends Role> getRoles() {
		return new TraversalResult<>(out(HAS_USER).in(HAS_ROLE).frameExplicit(RoleImpl.class));
	}

	@Override
	public TraversalResult<? extends Role> getRolesViaShortcut() {
		// TODO Use shortcut index.
		return out(ASSIGNED_TO_ROLE, RoleImpl.class);
	}

	@Override
	public Page<? extends Role> getRolesViaShortcut(User user, PagingParameters params) {
		String indexName = "e." + ASSIGNED_TO_ROLE + "_out";
		return new DynamicTransformablePageImpl<>(user, indexName.toLowerCase(), id(), Direction.IN, RoleImpl.class, params, READ_PERM, null, true);
	}

	@Override
	public void updateShortcutEdges() {
		GroupRoot groupRoot = Tx.get().data().groupDao();
		outE(ASSIGNED_TO_ROLE).removeAll();
		for (Group group : getGroups()) {
			for (Role role : groupRoot.getRoles(group)) {
				setUniqueLinkOutTo(role, ASSIGNED_TO_ROLE);
			}
		}
	}

	@Override
	public Node getReferencedNode() {
		return out(HAS_NODE_REFERENCE, NodeImpl.class).nextOrNull();
	}

	@Override
	public User setReferencedNode(Node node) {
		setUniqueLinkOutTo(node, HAS_NODE_REFERENCE);
		return this;
	}

	@Override
	public UserReference transformToReference() {
		return new UserReference().setFirstName(getFirstname()).setLastName(getLastname()).setUuid(getUuid());
	}

	@Deprecated
	@Override
	public UserResponse transformToRestSync(InternalActionContext ac, int level, String... languageTags) {
		UserDaoWrapper userDao = mesh().boot().userDao();
		return userDao.transformToRestSync(this, ac, level, languageTags);
	}

	@Override
	public User addGroup(Group group) {
		// Redirect to group implementation
		mesh().boot().groupDao().addUser(group, this);
		return this;
	}

	@Override
	public String getPasswordHash() {
		return property(PASSWORD_HASH_PROPERTY_KEY);
	}

	@Override
	public User setPasswordHash(String hash) {
		property(PASSWORD_HASH_PROPERTY_KEY, hash);
		// Password has changed, the user is not forced to change their password anymore.
		setForcedPasswordChange(false);
		return this;
	}

	@Override
	public void delete(BulkActionContext bac) {
		UserDaoWrapper userDao = mesh().boot().userDao();
		userDao.delete(this, bac);
	}

	@Override
	public boolean update(InternalActionContext ac, EventQueueBatch batch) {
		UserRoot userRoot = Tx.get().data().userDao();
		return userRoot.update(this, ac, batch);
	}

	@Override
	public String getSubETag(InternalActionContext ac) {
		UserDaoWrapper userRoot = mesh().boot().userDao();
		return userRoot.getSubETag(this, ac);
	}

	@Override
	public String getAPIPath(InternalActionContext ac) {
		return VersionHandler.baseRoute(ac) + "/users/" + getUuid();
	}

	@Override
	public User getCreator() {
		return mesh().userProperties().getCreator(this);
	}

	@Override
	public User getEditor() {
		return mesh().userProperties().getEditor(this);
	}

	@Override
	public MeshAuthUser toAuthUser() {
		return reframeExplicit(MeshAuthUserImpl.class);
	}

	@Override
	public String getAPIKeyTokenCode() {
		return property(API_TOKEN_ID);
	}

	@Override
	public User setAPITokenId(String code) {
		property(API_TOKEN_ID, code);
		return this;
	}

	@Override
	public Long getAPITokenIssueTimestamp() {
		return property(API_TOKEN_ISSUE_TIMESTAMP);
	}

	@Override
	public User setAPITokenIssueTimestamp() {
		setAPITokenIssueTimestamp(System.currentTimeMillis());
		return this;
	}

	@Override
	public User setAPITokenIssueTimestamp(Long timestamp) {
		property(API_TOKEN_ISSUE_TIMESTAMP, timestamp);
		return this;
	}

}
