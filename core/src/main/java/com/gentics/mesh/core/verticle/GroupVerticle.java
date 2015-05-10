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
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractCoreApiVerticle;
import com.gentics.mesh.core.data.model.MeshRoot;
import com.gentics.mesh.core.data.model.auth.MeshPermission;
import com.gentics.mesh.core.data.model.auth.Group;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.auth.Role;
import com.gentics.mesh.core.data.model.auth.User;
import com.gentics.mesh.core.rest.common.response.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.request.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.request.GroupUpdateRequest;
import com.gentics.mesh.core.rest.group.response.GroupListResponse;
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
		// addGroupChildGroupHandlers();
		addGroupUserHandlers();
		addGroupRoleHandlers();

		addCreateHandler();
		addReadHandler();
		addUpdateHandler();
		addDeleteHandler();
	}

	private void addGroupRoleHandlers() {
		route("/:groupUuid/roles/:roleUuid").method(POST).produces(APPLICATION_JSON).handler(rc -> {

			rcs.loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
					Group group = grh.result();
					Role role = rrh.result();
					if (group.addRole(role)) {
						group = groupService.save(group);
					}
				}, trh -> {
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
					if (group.removeRole(role)) {
						group = groupService.save(group);
					}
				}, trh -> {
					Group group = grh.result();
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, group)));
				});
			});
		});
	}

	private void addGroupUserHandlers() {
		Route route = route("/:groupUuid/users/:userUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {

			rcs.loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				rcs.loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
					Group group = grh.result();
					User user = urh.result();
					if (group.addUser(user)) {
						group = groupService.save(group);
					}
				}, trh -> {
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
					if (group.removeUser(user)) {
						groupService.save(group);
					}
				}, trh -> {
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

				group = groupService.save(group);
			}, trh -> {
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
				Page<Group> groupPage = groupService.findAllVisible(user, pagingInfo);
				for (Group group : groupPage) {
					listResponse.getData().add(groupService.transformToRest(rc, group));
				}
				RestModelPagingHelper.setPaging(listResponse, groupPage, pagingInfo);
				glr.complete(listResponse);
			}, ar -> {
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
				Group group = new Group(requestModel.getName());
				group = groupService.save(group);
				roleService.addCRUDPermissionOnRole(rc, new MeshPermission(root.getGroupRoot(), PermissionType.CREATE), group);
				groupCreated.complete(group);
			}, tch -> {
				Group createdGroup = groupCreated.result();
				rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(rc, createdGroup)));
			});

		});

	}
}
