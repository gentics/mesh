package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
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

import com.gentics.cailun.core.AbstractCoreApiVerticle;
import com.gentics.cailun.core.data.model.CaiLunRoot;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.common.response.GenericMessageResponse;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.core.rest.group.response.GroupListResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.paging.PagingInfo;
import com.gentics.cailun.util.RestModelPagingHelper;





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
			int depth = getDepth(rc);

			loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
					Group group = grh.result();
					Role role = rrh.result();
					if (group.addRole(role)) {
						group = groupService.save(group);
					}
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group, depth)));
				});
			});

		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			int depth = getDepth(rc);

			loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				Group group = grh.result();
				loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
					Role role = rrh.result();
					Group savedGroup = group;

					if (group.removeRole(role)) {
						savedGroup = groupService.save(group);
					}
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(savedGroup, depth)));
				});
			});
		});
	}

	private void addGroupUserHandlers() {
		Route route = route("/:groupUuid/users/:userUuid").method(POST).produces(APPLICATION_JSON);
		route.handler(rc -> {
			int depth = getDepth(rc);

			loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
					Group group = grh.result();
					User user = urh.result();
					Group savedGroup = group;
					if (group.addUser(user)) {
						savedGroup = groupService.save(group);
					}
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(savedGroup, depth)));
				});
			});
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE).produces(APPLICATION_JSON);
		route.handler(rc -> {
			int depth = getDepth(rc);

			loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
					Group group = grh.result();
					User user = urh.result();
					if (group.removeUser(user)) {
						groupService.save(group);
					}
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group, depth)));
				});
			});
		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).produces(APPLICATION_JSON).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Group> grh) -> {
				Group group = grh.result();
				groupService.delete(group);
				rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "group_deleted", uuid))));
			});
		});
	}

	// TODO Determine what we should do about conflicting group names. Should we let neo4j handle those cases?
	// TODO update timestamps
	private void addUpdateHandler() {
		route("/:uuid").method(PUT).consumes(APPLICATION_JSON).produces(APPLICATION_JSON).handler(rc -> {
			loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
				int depth = getDepth(rc);

				Group group = grh.result();
				GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
				}

				if (!group.getName().equals(requestModel.getName())) {
					Group groupWithSameName = groupService.findByName(requestModel.getName());
					if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
						throw new HttpStatusCodeErrorException(400, i18n.get(rc, "group_conflicting_name"));
					}
					group.setName(requestModel.getName());
				}

				group = groupService.save(group);
				rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group, depth)));
			});

		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			int depth = getDepth(rc);

			loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Group> grh) -> {
				Group group = grh.result();
				rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group, depth)));
			});
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).produces(APPLICATION_JSON).handler(rc -> {
			
			PagingInfo pagingInfo = getPagingInfo(rc);
			int depth = getDepth(rc);

			vertx.executeBlocking((Future<GroupListResponse> glr )-> {
				GroupListResponse listResponse = new GroupListResponse();
				User user = userService.findUser(rc);
				Page<Group> groupPage = groupService.findAllVisible(user, pagingInfo);
				for (Group group : groupPage) {
					listResponse.getData().add(groupService.transformToRest(group, depth));
				}
				RestModelPagingHelper.setPaging(listResponse, groupPage, pagingInfo);
				glr.complete(listResponse);
			}, (AsyncResult<GroupListResponse> ar) -> {
				rc.response().setStatusCode(200).end(toJson(ar.result()));
			});
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

			GroupCreateRequest requestModel = fromJson(rc, GroupCreateRequest.class);

			if (StringUtils.isEmpty(requestModel.getName())) {
				throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
			}

			int depth = getDepth(rc);

			CaiLunRoot root = cailunRootService.findRoot();
			hasPermission(rc, root.getGroupRoot(), PermissionType.CREATE, rh -> {
				// TODO handle conflicting group name: group_conflicting_name
					Group group = new Group(requestModel.getName());
					group = groupService.save(group);
					roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(root.getGroupRoot(), PermissionType.CREATE), group);
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group, depth)));
				});

		});

	}
}
