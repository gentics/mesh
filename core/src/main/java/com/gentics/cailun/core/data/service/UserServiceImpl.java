package com.gentics.cailun.core.data.service;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.impl.LoggerFactory;
import io.vertx.ext.apex.RoutingContext;

import java.util.HashSet;
import java.util.Set;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.traversal.Uniqueness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.auth.AuthRelationships;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.GraphPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.service.generic.GenericNodeServiceImpl;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.etc.CaiLunSpringConfiguration;
import com.gentics.cailun.path.PagingInfo;

@Component
@Transactional(readOnly = true)
public class UserServiceImpl extends GenericNodeServiceImpl<User> implements UserService {

	private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

	@Autowired
	private CaiLunSpringConfiguration springConfiguration;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private GraphDatabaseService graphDb;

	@Override
	public void setPassword(User user, String password) {
		user.setPasswordHash(springConfiguration.passwordEncoder().encode(password));
	}

	@Override
	public Page<User> findAllVisible(User requestUser, PagingInfo pagingInfo) {
		return userRepository.findAll(requestUser, new PageRequest(pagingInfo.getPage(), pagingInfo.getPerPage()));
	}

	@Override
	public User findByUsername(String username) {
		return userRepository.findByUsername(username);
	}

	@Override
	public UserResponse transformToRest(User user) {
		if (user == null) {
			throw new HttpStatusCodeErrorException(500, "User can't be null");
		}
		UserResponse restUser = new UserResponse();
		restUser.setUuid(user.getUuid());
		restUser.setUsername(user.getUsername());
		restUser.setEmailAddress(user.getEmailAddress());
		restUser.setFirstname(user.getFirstname());
		restUser.setLastname(user.getLastname());

		for (Group group : user.getGroups()) {
			restUser.addGroup(group.getName());
		}
		return restUser;
	}

	@Override
	public boolean removeUserFromGroup(User user, Group group) {
		return group.removeUser(user);
	}

	@Override
	public Set<GraphPermission> findGraphPermissions(User user, AbstractPersistable node) {

		Set<GraphPermission> permissions = new HashSet<>();
		Node userNode = neo4jTemplate.getPersistentState(user);

		// Traverse the graph from user to the page. Collect all permission relations and check them individually
		for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
				.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
				.relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
				.traverse(userNode).relationships()) {
			// log.info("Found Relationship " + rel.getType().name() + " between: " + rel.getEndNode().getId() + rel.getEndNode().getLabels() + " and "
			// + rel.getStartNode().getId() + rel.getStartNode().getLabels());

			if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getType().name())) {
				// Check whether this relation in fact targets our object we want to check
				boolean matchesTargetNode = rel.getEndNode().getId() == node.getId();
				if (matchesTargetNode) {
					// Convert the api relationship to a SDN relationship
					GraphPermission perm = neo4jTemplate.load(rel, GraphPermission.class);
					permissions.add(perm);
				}
			}
		}
		return permissions;

	}

	public boolean isPermitted(long userNodeId, CaiLunPermission genericPermission) throws Exception {
		if (genericPermission.getTargetNode() == null) {
			return false;
		}
		Node userNode = graphDb.getNodeById(userNodeId);
		for (Relationship groupRel : userNode.getRelationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)) {
			Node group = groupRel.getEndNode();
			log.debug("Found group: " + group.getProperty("name"));
			for (Relationship roleRel : group.getRelationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)) {
				Node role = roleRel.getStartNode();
				log.debug("Found role: " + role.getProperty("name"));
				for (Relationship authRel : role.getRelationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING)) {
					log.debug("Permission from {" + authRel.getStartNode().getId() + " to " + authRel.getEndNode().getId());
					boolean matchesTargetNode = authRel.getEndNode().getId() == genericPermission.getTargetNode().getId();
					if (matchesTargetNode) {
						log.debug("Found permission");
						// Convert the api relationship to a SDN relationship
						GraphPermission perm = neo4jTemplate.load(authRel, GraphPermission.class);
						if (genericPermission.implies(perm) == true) {
							return true;
						}
					}
				}
			}
		}

//		// Traverse the graph from user to the page. Collect all permission relations and check them individually
//		for (Relationship rel : graphDb.traversalDescription().depthFirst().relationships(AuthRelationships.TYPES.MEMBER_OF, Direction.OUTGOING)
//				.relationships(AuthRelationships.TYPES.HAS_ROLE, Direction.INCOMING)
//				.relationships(AuthRelationships.TYPES.HAS_PERMISSION, Direction.OUTGOING).uniqueness(Uniqueness.RELATIONSHIP_GLOBAL)
//				.traverse(userNode).relationships()) {
//			// log.debug("Found Relationship " + rel.getType().name() + " between: " + rel.getEndNode().getId() + rel.getEndNode().getLabels() + " and "
//			// + rel.getStartNode().getId() + rel.getStartNode().getLabels());
//
//			if (AuthRelationships.HAS_PERMISSION.equalsIgnoreCase(rel.getType().name())) {
//				// Check whether this relation in fact targets our object we want to check
//				boolean matchesTargetNode = rel.getEndNode().getId() == genericPermission.getTargetNode().getId();
//				if (matchesTargetNode) {
//					// Convert the api relationship to a SDN relationship
//					GraphPermission perm = neo4jTemplate.load(rel, GraphPermission.class);
//					if (genericPermission.implies(perm) == true) {
//						return true;
//					}
//				}
//			}
//		}
		return false;
	}

	public String[] getPerms(RoutingContext rc, AbstractPersistable node) {
//		User user = springConfiguration.authService().getUser(rc);
		User user = null;
		Set<GraphPermission> permissions = findGraphPermissions(user, node);
		Set<String> perms = new HashSet<>();
		for (GraphPermission perm : permissions) {
			if (perm.isPermitted(PermissionType.READ)) {
				perms.add(PermissionType.READ.getPropertyName());
			}
			if (perm.isPermitted(PermissionType.DELETE)) {
				perms.add(PermissionType.DELETE.getPropertyName());
			}
			if (perm.isPermitted(PermissionType.CREATE)) {
				perms.add(PermissionType.CREATE.getPropertyName());
			}
			if (perm.isPermitted(PermissionType.UPDATE)) {
				perms.add(PermissionType.UPDATE.getPropertyName());
			}
		}
		return perms.toArray(new String[perms.size()]);
	}

}
