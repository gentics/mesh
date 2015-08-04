package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static com.gentics.mesh.util.VerticleHelper.hasSucceeded;
import static com.gentics.mesh.util.VerticleHelper.loadObject;
import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.web.Route;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.verticle.handler.GroupCRUDHandler;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.InvalidArgumentException;

@Component
@Scope("singleton")
@SpringVerticle
public class GroupVerticle extends AbstractCoreApiVerticle {

	@Autowired
	private GroupCRUDHandler crudHandler;

	public GroupVerticle() {
		super("groups");
	}

	@Override
	public void registerEndPoints() throws Exception {
		route("/*").handler(springConfiguration.authHandler());
		addGroupUserHandlers();
		addGroupRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupRoleHandlers() {

		route("/:groupUuid/roles").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			PagingInfo pagingInfo = getPagingInfo(rc);
			MeshAuthUser requestUser = getUser(rc);

			loadObject(rc, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {
				try {
					Page<? extends Role> rolePage = grh.result().getRoles(requestUser, pagingInfo);
					transformAndResponde(rc, rolePage, new RoleListResponse());
				} catch (InvalidArgumentException e) {
					rc.fail(e);
				}
			});

		});

		route("/:groupUuid/roles/:roleUuid").method(POST).produces(APPLICATION_JSON).handler(rc -> {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					loadObject(rc, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(rc, rrh)) {
							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
								Group group = grh.result();
								Role role = rrh.result();
								group.addRole(role);
								tx.success();
								transformAndResponde(rc, group);
							}
						}
					});
				}
			});

		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					// TODO check whether the role is actually part of the group
					loadObject(rc, "roleUuid", READ_PERM, boot.roleRoot(), rrh -> {
						if (hasSucceeded(rc, rrh)) {
							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
								Group group = grh.result();
								Role role = rrh.result();
								group.removeRole(role);
								tx.success();
								transformAndResponde(rc, group);
							}
						}
					});

				}
			});
		});
	}

	private void addGroupUserHandlers() {
		route("/:groupUuid/users").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			PagingInfo pagingInfo = getPagingInfo(rc);

			loadObject(rc, "groupUuid", READ_PERM, boot.groupRoot(), grh -> {

				if (hasSucceeded(rc, grh)) {
					Group group = grh.result();
					Page<? extends User> userPage;
					try {
						userPage = group.getVisibleUsers(requestUser, pagingInfo);
						transformAndResponde(rc, userPage, new UserListResponse());
					} catch (Exception e) {
						rc.fail(e);
					}
				}
			});

		});

		Route route = route("/:groupUuid/users/:userUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(rc, urh)) {
							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
								Group group = grh.result();
								User user = urh.result();
								group.addUser(user);
								tx.success();
							}
							Group group = grh.result();
							transformAndResponde(rc, group);
						}
					});
				}
			});
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
						if (hasSucceeded(rc, urh)) {
							try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
								Group group = grh.result();
								User user = urh.result();
								group.removeUser(user);
								tx.success();
							}
							Group group = grh.result();
							transformAndResponde(rc, group);
						}
					});
				}
			});
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleDelete(rc);
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleUpdate(rc);
		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleRead(rc);
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			crudHandler.handleReadList(rc);
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {
			crudHandler.handleCreate(rc);
		});

	}
}
