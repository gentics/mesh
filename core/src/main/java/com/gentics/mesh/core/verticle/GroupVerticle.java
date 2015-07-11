package com.gentics.mesh.core.verticle;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.json.JsonUtil.fromJson;
import static com.gentics.mesh.json.JsonUtil.toJson;
import static com.gentics.mesh.util.RoutingContextHelper.getPagingInfo;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
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
import com.gentics.mesh.core.data.impl.GroupImpl;
import com.gentics.mesh.core.data.impl.RoleImpl;
import com.gentics.mesh.core.data.impl.UserImpl;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupListResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.util.BlueprintTransaction;
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

			rcs.loadObject(rc, "groupUuid", READ_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				vertx.executeBlocking((Future<RoleListResponse> bch) -> {
					RoleListResponse listResponse = new RoleListResponse();
					Group group = grh.result();
					Page<? extends Role> rolePage;
					try {
						rolePage = group.getRoles(requestUser, pagingInfo);
						for (Role role : rolePage) {
							listResponse.getData().add(role.transformToRest(getUser(rc)));
						}
						RestModelPagingHelper.setPaging(listResponse, rolePage);
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
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "groupUuid", UPDATE_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "roleUuid", READ_PERM, RoleImpl.class, (AsyncResult<Role> rrh) -> {
					Group group = grh.result();
					Role role = rrh.result();
					group.addRole(role);
					// group = groupRoot.save(group);

					}, trh -> {
						if (trh.failed()) {
							rc.fail(trh.cause());
						}
						Group group = grh.result();
						rc.response().setStatusCode(200).end(toJson(group.transformToRest(requestUser)));
					});
			});

		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "groupUuid", UPDATE_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "roleUuid", READ_PERM, RoleImpl.class, (AsyncResult<Role> rrh) -> {
					Group group = grh.result();
					Role role = rrh.result();
					group.removeRole(role);
					// group = groupRoot.save(group);
					}, trh -> {
						if (trh.failed()) {
							rc.fail(trh.cause());
						}
						Group group = grh.result();
						rc.response().setStatusCode(200).end(toJson(group.transformToRest(requestUser)));
					});
			});
		});
	}

	private void addGroupUserHandlers() {
		route("/:groupUuid/users").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			PagingInfo pagingInfo = getPagingInfo(rc);
			rcs.loadObject(rc, "groupUuid", READ_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				vertx.executeBlocking((Future<UserListResponse> bch) -> {
					UserListResponse listResponse = new UserListResponse();
					Group group = grh.result();
					Page<? extends User> userPage;
					try {
						userPage = group.getVisibleUsers(requestUser, pagingInfo);
						for (User user : userPage) {
							listResponse.getData().add(user.transformToRest(requestUser));
						}
						RestModelPagingHelper.setPaging(listResponse, userPage);

						bch.complete(listResponse);
					} catch (Exception e) {
						rc.fail(e);
					}
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
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "groupUuid", UPDATE_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "userUuid", READ_PERM, UserImpl.class, (AsyncResult<User> urh) -> {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						Group group = grh.result();
						User user = urh.result();
						group.addUser(user);
						tx.success();
					}
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					Group group = grh.result();
					rc.response().setStatusCode(200).end(toJson(group.transformToRest(requestUser)));
				});
			});
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "groupUuid", UPDATE_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "userUuid", READ_PERM, UserImpl.class, (AsyncResult<User> urh) -> {
					try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
						Group group = grh.result();
						User user = urh.result();
						group.removeUser(user);
						tx.success();
					}
				}, trh -> {
					if (trh.failed()) {
						rc.fail(trh.cause());
					}
					Group group = grh.result();
					rc.response().setStatusCode(200).end(toJson(group.transformToRest(requestUser)));
				});
			});
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			rcs.loadObject(rc, "uuid", DELETE_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
				try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
					Group group = grh.result();
					group.delete();
					tx.success();
				}
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
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", UPDATE_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {
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

			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Group group = trh.result();
				rc.response().setStatusCode(200).end(toJson(group.transformToRest(requestUser)));
			});

		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			MeshAuthUser requestUser = getUser(rc);

			rcs.loadObject(rc, "uuid", READ_PERM, GroupImpl.class, (AsyncResult<Group> grh) -> {  
			}, trh -> {
				if (trh.failed()) {
					rc.fail(trh.cause());
				}
				Group group = trh.result();
				rc.response().setStatusCode(200).end(toJson(group.transformToRest(requestUser)));
			});
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {

			PagingInfo pagingInfo = getPagingInfo(rc);

			vertx.executeBlocking((Future<GroupListResponse> glr) -> {
				GroupListResponse listResponse = new GroupListResponse();
				MeshAuthUser requestUser = getUser(rc);

				Page<? extends Group> groupPage;
				try {
					groupPage = boot.groupRoot().findAll(requestUser, pagingInfo);
					for (Group group : groupPage) {
						listResponse.getData().add(group.transformToRest(requestUser));
					}
					RestModelPagingHelper.setPaging(listResponse, groupPage);
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
			MeshAuthUser requestUser = getUser(rc);
			GroupCreateRequest requestModel = JsonUtil.fromJson(rc, GroupCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				rc.fail(new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set")));
				return;
			}

			Future<Group> groupCreated = Future.future();

			MeshRoot root = boot.meshRoot();
			GroupRoot groupRoot = root.getGroupRoot();
			rcs.hasPermission(rc, groupRoot, CREATE_PERM, rh -> {
				Group group = groupRoot.create(requestModel.getName());
				requestUser.addCRUDPermissionOnRole(root.getGroupRoot(), CREATE_PERM, group);
				groupCreated.complete(group);
			}, tch -> {
				if (tch.failed()) {
					rc.fail(tch.cause());
				}
				Group createdGroup = groupCreated.result();
				rc.response().setStatusCode(200).end(toJson(createdGroup.transformToRest(requestUser)));
			});

		});

	}
}
