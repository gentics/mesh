package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.util.JsonUtils.fromJson;
import static com.gentics.mesh.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.root.MeshRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.request.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.request.GroupUpdateRequest;
import com.gentics.mesh.core.rest.group.response.GroupListResponse;
import com.gentics.mesh.core.rest.role.response.RoleListResponse;
import com.gentics.mesh.core.rest.user.response.UserListResponse;
import com.gentics.mesh.error.HttpStatusCodeErrorException;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.util.RestModelPagingHelper;

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
		addGroupTPRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupTPRoleHandlers() {

		route("/:groupUuid/roles").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			PagingInfo pagingInfo = rcs.getPagingInfo(rc);
			rcs.loadObject(rc, "groupUuid", PermissionType.READ, (AsyncResult<Group> grh) -> {
				vertx.executeBlocking((Future<RoleListResponse> bch) -> {
					RoleListResponse listResponse = new RoleListResponse();
					Group group = grh.result();
					Page<? extends Role> rolePage;
					try {
						rolePage = roleService.findByGroup(rc, group, pagingInfo);
						for (Role role : rolePage) {
							listResponse.getData().add(roleService.transformToRest(role));
						}
						//TODO  fix paging 
						//					RestModelPagingHelper.setPaging(listResponse, rolePage, pagingInfo);
						bch.complete(listResponse);
					} catch (Exception e) {
						bch.fail(e);
					}

				}, rh -> {
					if (rh.failed()) {
						throw new RuntimeException(rh.cause());
					}
					RoleListResponse listResponse = rh.result();
					rc.response().setStatusCode(200).end(toJson(listResponse));
				});

			});

		});

		route("/:groupUuid/roles/:roleUuid").method(POST).produces(APPLICATION_JSON).handler(rc -> {

			rcs.loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
					Group group = grh.result();
					Role role = rrh.result();
					group.addRole(role);
					//	group = groupService.save(group);

					}, trh -> {
						if (trh.failed()) {
							rc.fail(trh.cause());
						}
						Group group = grh.result();
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
					});
			});

		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {

			rcs.loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
					Group group = grh.result();
					Role role = rrh.result();
					group.removeRole(role);
					//group = groupService.save(group);
					}, trh -> {
						if (trh.failed()) {
							rc.fail(trh.cause());
						}
						Group group = grh.result();
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
					});
			});
		});
	}

	private void addGroupUserHandlers() {
		route("/:groupUuid/users").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			PagingInfo pagingInfo = rcs.getPagingInfo(rc);
			rcs.loadObject(rc, "groupUuid", PermissionType.READ, (AsyncResult<Group> grh) -> {
				vertx.executeBlocking((Future<UserListResponse> bch) -> {
					UserListResponse listResponse = new UserListResponse();
					Group group = grh.result();
					Page<User> userPage = userService.findByGroup(rc, group, pagingInfo);
					for (User user : userPage) {
						listResponse.getData().add(userService.transformToRest(user));
					}
					RestModelPagingHelper.setPaging(listResponse, userPage, pagingInfo);

					bch.complete(listResponse);
				}, rh -> {
					if (rh.failed()) {
						rc.fail(rh.cause());
					}
					UserListResponse listResponse = rh.result();
					rc.response().setStatusCode(200).end(toJson(listResponse));
				});

			});

		});

		Route route = route("/:groupUuid/users/:userUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {

			rcs.loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
					Group group = grh.result();
					User user = urh.result();
					group.addUser(user);
					//group = groupService.save(group);
					}, trh -> {
						if (trh.failed()) {
							rc.fail(trh.cause());
						}
						Group group = grh.result();
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
					});
			});
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			rcs.loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
					Group group = grh.result();
					User user = urh.result();
					group.removeUser(user);
					//groupService.save(group);

					}, trh -> {
						if (trh.failed()) {
							rc.fail(trh.cause());
						}
						Group group = grh.result();
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
					});
			});
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			rcs.loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Group> grh) -> {
				Group group = grh.result();
				groupService.delete(group);
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "group_deleted", uuid))));
			});
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				Group group = grh.result();
				GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
					return;
				}

				if (!group.getName().equals(requestModel.getName())) {
					Group groupWithSameName = groupService.findByName(requestModel.getName());
					if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
						rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "group_conflicting_name")));
						return;
					}
					group.setName(requestModel.getName());
				}

			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Group group = trh.result();
				rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
			});

		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			rcs.loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Group> grh) -> {
				Group group = grh.result();
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Group group = trh.result();
				rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
			});
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {

			PagingInfo pagingInfo = rcs.getPagingInfo(rc);

			vertx.executeBlocking((Future<GroupListResponse> glr) -> {
				GroupListResponse listResponse = new GroupListResponse();
				User user = userService.findUser(rc);
				Page<? extends Group> groupPage;
				try {
					groupPage = groupService.findAllVisible(user, pagingInfo);
					for (Group group : groupPage) {
						listResponse.getData().add(groupService.transformToRest(rc, group));
					}
					RestModelPagingHelper.setPaging(listResponse, groupPage, pagingInfo);
					glr.complete(listResponse);

				} catch (Exception e) {
					glr.fail(e);
				}
			}, ar -> {
				if (ar.failed()) {
					rc.fail(ar.cause());
				}
				rc.response().setStatusCode(200).end(toJson(ar.result()));
			});
		});
	}

	// TODO handle conflicting group name: group_conflicting_name
	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

			GroupCreateRequest requestModel = fromJson(rc, GroupCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
				return;
			}

			Future<Group> groupCreated = Future.future();

			MeshRoot root = meshRootService.findRoot();
			rcs.hasPermission(rc, root.getGroupRoot(), PermissionType.CREATE, rh -> {
				Group group = groupService.create(requestModel.getName());
				roleService.addCRUDPermissionOnRole(rc, new MeshPermission(root.getGroupRoot(), PermissionType.CREATE), group);
				groupCreated.complete(group);
			}, tch -> {
				if (tch.failed()) {
					rc.fail(tch.cause());
				}
				Group createdGroup = groupCreated.result();
				rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, createdGroup)));
			});

		});

	}
}
