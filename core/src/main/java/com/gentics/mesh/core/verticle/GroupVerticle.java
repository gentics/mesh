package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.ext.web.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.error.InvalidPermissionException;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;
import com.gentics.mesh.util.InvalidArgumentException;
import static io.netty.handler.codec.http.HttpResponseStatus.*;

@Component
@Scope("singleton")
@SpringVerticle
public class GroupVerticle extends AbstractCoreApiVerticle {

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
								transformAndResponde(rc, role);
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
				loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						Group group = grh.result();
						User user = urh.result();
						group.addUser(user);
						tx.success();
					}
					Group group = grh.result();
					transformAndResponde(rc, group);
				});
			});
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			loadObject(rc, "groupUuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				loadObject(rc, "userUuid", READ_PERM, boot.userRoot(), urh -> {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						Group group = grh.result();
						User user = urh.result();
						group.removeUser(user);
						tx.success();
					}
					Group group = grh.result();
					transformAndResponde(rc, group);
				});
			});
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			delete(rc, "uuid", "group_deleted", boot.groupRoot());
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			loadObject(rc, "uuid", UPDATE_PERM, boot.groupRoot(), grh -> {
				if (hasSucceeded(rc, grh)) {
					Group group = grh.result();
					GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

					if (StringUtils.isEmpty(requestModel.getName())) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
						return;
					}

					if (!group.getName().equals(requestModel.getName())) {
						Group groupWithSameName = boot.groupRoot().findByName(requestModel.getName());
						if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
							rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "group_conflicting_name")));
							return;
						}
						group.setName(requestModel.getName());
					}
					transformAndResponde(rc, group);
				}
			});

		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			loadTransformAndResponde(rc, "uuid", READ_PERM, boot.groupRoot());
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			loadTransformAndResponde(rc, boot.groupRoot(), new GroupListResponse());
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);
			GroupCreateRequest requestModel = JsonUtil.fromJson(rc, GroupCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
				return;
			}

			MeshRoot root = boot.meshRoot();
			GroupRoot groupRoot = root.getGroupRoot();
			if (requestUser.hasPermission(groupRoot, CREATE_PERM)) {
				if (groupRoot.findByName(requestModel.getName()) != null) {
					rc.fail(new HttpStatusCodeErrorException(CONFLICT, i18n.get(rc, "group_conflicting_name")));
				} else {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						Group group = groupRoot.create(requestModel.getName());
						requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
						tx.success();
						transformAndResponde(rc, group);
					}
				}
			} else {
				rc.fail(new InvalidPermissionException(i18n.get(rc, "error_missing_perm", groupRoot.getUuid())));
			}

		});

	}
}
