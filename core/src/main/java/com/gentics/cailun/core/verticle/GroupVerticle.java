package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.util.JsonUtils.fromJson;
import static com.gentics.cailun.util.JsonUtils.toJson;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.AsyncResult;
import io.vertx.ext.apex.Route;

import org.apache.commons.lang3.StringUtils;
import org.jacpfx.vertx.spring.SpringVerticle;
import org.neo4j.graphdb.Transaction;
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
		route("/:groupUuid/roles/:roleUuid").method(POST).handler(rc -> {

			try (Transaction tx = graphDb.beginTx()) {
				loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}

					loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
						if (rrh.failed()) {
							rc.fail(rrh.cause());
						}
						Group group = grh.result();

						Role role = rrh.result();
						if (group.addRole(role)) {
							group = groupService.save(group);
						}
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group)));
					});
				});
				tx.success();
			}

		});

		route("/:groupUuid/roles/:roleUuid").method(DELETE).handler(rc -> {
			try (Transaction tx = graphDb.beginTx()) {
				loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}
					Group group = grh.result();
					loadObject(rc, "roleUuid", PermissionType.READ, (AsyncResult<Role> rrh) -> {
						if (rrh.failed()) {
							rc.fail(rrh.cause());
						}
						Role role = rrh.result();
						Group savedGroup = group;

						if (group.removeRole(role)) {
							savedGroup = groupService.save(group);
						}
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(savedGroup)));
					});
				});
				tx.success();
			}
		});
	}

	private void addGroupUserHandlers() {
		Route route = route("/:groupUuid/users/:userUuid").method(POST);
		route.handler(rc -> {
			try (Transaction tx = graphDb.beginTx()) {

				loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}
					Group group = grh.result();
					loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
						if (urh.failed()) {
							rc.fail(urh.cause());
						}
						User user = urh.result();
						Group savedGroup = group;
						if (group.addUser(user)) {
							savedGroup = groupService.save(group);
						}
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(savedGroup)));
					});
				});
				tx.success();
			}
		});

		route = route("/:groupUuid/users/:userUuid").method(DELETE);
		route.handler(rc -> {
			try (Transaction tx = graphDb.beginTx()) {

				loadObject(rc, "groupUuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}
					Group group = grh.result();
					loadObject(rc, "userUuid", PermissionType.READ, (AsyncResult<User> urh) -> {
						if (urh.failed()) {
							rc.fail(urh.cause());
						}
						User user = urh.result();

						if (group.removeUser(user)) {
							groupService.save(group);
						}
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group)));
					});
				});
				tx.success();
			}

		});
	}

	private void addDeleteHandler() {
		route("/:uuid").method(DELETE).handler(rc -> {
			String uuid = rc.request().params().get("uuid");
			try (Transaction tx = graphDb.beginTx()) {
				loadObject(rc, "uuid", PermissionType.DELETE, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}
					Group group = grh.result();
					groupService.delete(group);
					rc.response().setStatusCode(200).end(toJson(new GenericMessageResponse(i18n.get(rc, "group_deleted", uuid))));
				});
				tx.success();
			}

		});
	}

	private void addUpdateHandler() {
		route("/:uuid").method(PUT).handler(rc -> {
			try (Transaction tx = graphDb.beginTx()) {
				loadObject(rc, "uuid", PermissionType.UPDATE, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}
					Group group = grh.result();
					GroupUpdateRequest requestModel = fromJson(rc, GroupUpdateRequest.class);

					if (StringUtils.isEmpty(requestModel.getName())) {
						throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
					}

					// TODO should we keep this? I don't think so since group save would fail anyway. Let the index handle this?
						if (!group.getName().equals(requestModel.getName())) {

							Group groupWithSameName = groupService.findByName(requestModel.getName());
							if (groupWithSameName != null && !groupWithSameName.getUuid().equals(group.getUuid())) {
								throw new HttpStatusCodeErrorException(400, i18n.get(rc, "group_conflicting_name"));
							}
							group.setName(requestModel.getName());
						}

						// TODO update timestamps
						group = groupService.save(group);
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group)));
					});
				tx.success();
			}

		});

	}

	private void addReadHandler() {
		route("/:uuid").method(GET).handler(rc -> {
			try (Transaction tx = graphDb.beginTx()) {
				loadObject(rc, "uuid", PermissionType.READ, (AsyncResult<Group> grh) -> {
					if (grh.failed()) {
						rc.fail(grh.cause());
					}
					Group group = grh.result();
					rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group)));
				});
				tx.success();
			}
		});

		/*
		 * List all groups when no parameter was specified
		 */
		route("/").method(GET).handler(rc -> {
			GroupListResponse listResponse = new GroupListResponse();
			try (Transaction tx = graphDb.beginTx()) {
				PagingInfo pagingInfo = getPagingInfo(rc);

				// User requestUser = rc.session().getUser();
				User user = null;
				Page<Group> groupPage = groupService.findAllVisible(user, pagingInfo);
				for (Group group : groupPage) {
					listResponse.getData().add(groupService.transformToRest(group));
				}
				RestModelPagingHelper.setPaging(listResponse, groupPage, pagingInfo);
				tx.success();
			}
			rc.response().setStatusCode(200).end(toJson(listResponse));
		});
	}

	private void addCreateHandler() {
		route("/").method(POST).handler(rc -> {

			try (Transaction tx = graphDb.beginTx()) {
				GroupCreateRequest requestModel = fromJson(rc, GroupCreateRequest.class);

				if (StringUtils.isEmpty(requestModel.getName())) {
					throw new HttpStatusCodeErrorException(400, i18n.get(rc, "error_name_must_be_set"));
				}

				CaiLunRoot root = cailunRootService.findRoot();
				hasPermission(rc, root.getGroupRoot(), PermissionType.CREATE, rh -> {
					// TODO handle conflicting group name: group_conflicting_name
						Group group = new Group(requestModel.getName());
						group = groupService.save(group);
						roleService.addCRUDPermissionOnRole(rc, new CaiLunPermission(root.getGroupRoot(), PermissionType.CREATE), group);
						rc.response().setStatusCode(200).end(toJson(groupService.transformToRest(group)));
						tx.success();
					});

			}

		});

	}
}
