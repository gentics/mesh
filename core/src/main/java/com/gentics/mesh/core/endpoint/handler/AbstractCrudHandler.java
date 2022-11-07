package com.gentics.mesh.core.endpoint.handler;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.annotation.Getter;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.action.DAOActions;
import com.gentics.mesh.core.data.HibCoreElement;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;

/**
 * Abstract class for CRUD REST handlers. The abstract class provides handler methods for create, read (one), read (multiple) and delete.
 */
public abstract class AbstractCrudHandler<T extends HibCoreElement<RM>, RM extends RestModel> extends AbstractHandler implements CrudHandler {

	public static final String TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY = "rootElement";

	protected final Database db;
	protected final HandlerUtilities utils;
	protected final WriteLock writeLock;
	private final DAOActions<T, RM> actions;

	public AbstractCrudHandler(Database db, HandlerUtilities utils, WriteLock writeLock, DAOActions<T, RM> actions) {
		this.db = db;
		this.utils = utils;
		this.writeLock = writeLock;
		this.actions = actions;
	}

	@Getter
	public DAOActions<T, RM> crudActions() {
		return actions;
	}

	@Override
	public void handleCreate(InternalActionContext ac) {
		utils.createElement(ac, crudActions());
	}

	@Override
	public void handleDelete(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.deleteElement(ac, crudActions(), uuid);
	}

	@Override
	public void handleRead(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.readElement(ac, uuid, crudActions(), READ_PERM);
	}

	@Override
	public void handleUpdate(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.updateElement(ac, uuid, crudActions());
	}

	@Override
	public void handleReadList(InternalActionContext ac) {
		utils.readElementList(ac, crudActions());
	}

	/**
	 * Create a route handler which will load the element for the given uuid. The handler will only try to load the root element if a uuid was specified.
	 * Otherwise {@link RoutingContext#next()} will be invoked directly.
	 * 
	 * @param i18nNotFoundMessage
	 *            I18n error message that will be returned when no element could be found
	 */
	public Handler<RoutingContext> getUuidHandler(String i18nNotFoundMessage) {
		Handler<RoutingContext> handler = rc -> {
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			String uuid = ac.getParameter("param0");
			// Only try to load the root element when a uuid string was specified
			if (!isEmpty(uuid)) {
				boolean result = db.tx(tx -> {
					// TODO Calling load is not correct. The findByUuid method should be used here instead or the loadObject
					T foundElement = crudActions().loadByUuid(context(tx, ac), uuid, null, false);
					if (foundElement == null) {
						throw error(NOT_FOUND, i18nNotFoundMessage, uuid);
					} else {
						ac.data().put(TAGFAMILY_ELEMENT_CONTEXT_DATA_KEY, foundElement);
					}
					return true;
				});
				if (!result) {
					return;
				}
			}
			rc.next();
		};
		return handler;
	}

	/**
	 * Handle request to read permissions for all roles
	 * @param ac action context
	 * @param uuid entity uuid
	 */
	public void handleReadPermissions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");
		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			T object = crudActions().loadByUuid(context(tx, ac), uuid, READ_PERM, true);
			Set<HibRole> allRoles = roleDao.findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream().collect(Collectors.toSet());

			Map<HibRole, Set<InternalPermission>> permissions = roleDao.getPermissions(allRoles, object);
			permissions.values().removeIf(Set::isEmpty);

			ObjectPermissionResponse response = new ObjectPermissionResponse();
			permissions.entrySet().forEach(entry -> {
				RoleReference role = entry.getKey().transformToReference();
				entry.getValue().forEach(perm -> response.add(role, perm.getRestPerm()));
			});
			response.setOthers(object.hasPublishPermissions());

			return response;
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle request to grant permissions on sets of roles
	 * @param ac action context
	 * @param uuid entity uuid
	 */
	public void handleGrantPermissions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		ObjectPermissionGrantRequest update = ac.fromJson(ObjectPermissionGrantRequest.class);
		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibUser requestUser = ac.getUser();
			T object = crudActions().loadByUuid(context(tx, ac), uuid, READ_PERM, true);
			Set<HibRole> allRoles = roleDao.findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream().collect(Collectors.toSet());
			Map<String, HibRole> allRolesByUuid = allRoles.stream().collect(Collectors.toMap(HibRole::getUuid, Function.identity()));
			Map<String, HibRole> allRolesByName = allRoles.stream().collect(Collectors.toMap(HibRole::getName, Function.identity()));

			InternalPermission[] possiblePermissions = object.hasPublishPermissions()
					? InternalPermission.values()
					: InternalPermission.basicPermissions();

			for (InternalPermission perm : possiblePermissions) {
				List<RoleReference> roleRefsToSet = update.get(perm.getRestPerm());
				if (roleRefsToSet != null) {
					Set<HibRole> rolesToSet = new HashSet<>();
					for (RoleReference roleRef : roleRefsToSet) {
						// find the role for the role reference
						HibRole role = null;
						if (!StringUtils.isEmpty(roleRef.getUuid())) {
							role = allRolesByUuid.get(roleRef.getUuid());

							if (role == null) {
								throw error(NOT_FOUND, "object_not_found_for_uuid", roleRef.getUuid());
							}
						} else if (!StringUtils.isEmpty(roleRef.getName())) {
							role = allRolesByName.get(roleRef.getName());

							if (role == null) {
								throw error(NOT_FOUND, "object_not_found_for_name", roleRef.getName());
							}
						} else {
							throw error(BAD_REQUEST, "role_reference_uuid_or_name_missing");
						}

						// check update permission
						if (!userDao.hasPermission(requestUser, role, UPDATE_PERM)) {
							throw error(FORBIDDEN, "error_missing_perm", role.getUuid(), UPDATE_PERM.getRestPerm().getName());
						}

						rolesToSet.add(role);
					}

					roleDao.grantPermissions(rolesToSet, object, false, perm);

					// handle "exclusive" flag by revoking perm from all "other" roles
					if (update.isExclusive()) {
						// start with all roles, the user can see
						Set<HibRole> rolesToRevoke = new HashSet<>(allRoles);
						// remove all roles, which get the permission granted
						rolesToRevoke.removeAll(rolesToSet);

						// remove all roles, which should be ignored
						if (update.getIgnore() != null) {
							rolesToRevoke.removeIf(role -> {
								return update.getIgnore().stream().filter(ign -> {
									return StringUtils.equals(ign.getUuid(), role.getUuid()) || StringUtils.equals(ign.getName(), role.getName());
								}).findAny().isPresent();
							});
						}

						// remove all roles without UPDATE_PERM
						rolesToRevoke.removeIf(role -> !userDao.hasPermission(requestUser, role, UPDATE_PERM));

						if (!rolesToRevoke.isEmpty()) {
							roleDao.revokePermissions(rolesToRevoke, object, perm);
						}
					}
				}
			}

			Map<HibRole, Set<InternalPermission>> permissions = roleDao.getPermissions(allRoles, object);
			permissions.values().removeIf(Set::isEmpty);

			ObjectPermissionResponse response = new ObjectPermissionResponse();
			permissions.entrySet().forEach(entry -> {
				RoleReference role = entry.getKey().transformToReference();
				entry.getValue().forEach(perm -> response.add(role, perm.getRestPerm()));
			});
			response.setOthers(object.hasPublishPermissions());

			return response;
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle request to revoke permissions on sets of roles
	 * @param ac action context
	 * @param uuid entity uuid
	 */
	public void handleRevokePermissions(InternalActionContext ac, String uuid) {
		validateParameter(uuid, "uuid");

		ObjectPermissionRevokeRequest update = ac.fromJson(ObjectPermissionRevokeRequest.class);
		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibUser requestUser = ac.getUser();
			T object = crudActions().loadByUuid(context(tx, ac), uuid, READ_PERM, true);
			Set<HibRole> allRoles = roleDao.findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream().collect(Collectors.toSet());
			Map<String, HibRole> allRolesByUuid = allRoles.stream().collect(Collectors.toMap(HibRole::getUuid, Function.identity()));
			Map<String, HibRole> allRolesByName = allRoles.stream().collect(Collectors.toMap(HibRole::getName, Function.identity()));

			InternalPermission[] possiblePermissions = object.hasPublishPermissions()
					? InternalPermission.values()
					: InternalPermission.basicPermissions();

			for (InternalPermission perm : possiblePermissions) {
				List<RoleReference> roleRefsToRevoke = update.get(perm.getRestPerm());
				if (roleRefsToRevoke != null) {
					Set<HibRole> rolesToRevoke = new HashSet<>();
					for (RoleReference roleRef : roleRefsToRevoke) {
						// find the role for the role reference
						HibRole role = null;
						if (!StringUtils.isEmpty(roleRef.getUuid())) {
							role = allRolesByUuid.get(roleRef.getUuid());

							if (role == null) {
								throw error(NOT_FOUND, "object_not_found_for_uuid", roleRef.getUuid());
							}
						} else if (!StringUtils.isEmpty(roleRef.getName())) {
							role = allRolesByName.get(roleRef.getName());

							if (role == null) {
								throw error(NOT_FOUND, "object_not_found_for_name", roleRef.getName());
							}
						} else {
							throw error(BAD_REQUEST, "role_reference_uuid_or_name_missing");
						}

						// check update permission
						if (!userDao.hasPermission(requestUser, role, UPDATE_PERM)) {
							throw error(FORBIDDEN, "error_missing_perm", role.getUuid(), UPDATE_PERM.getRestPerm().getName());
						}

						rolesToRevoke.add(role);
					}

					roleDao.revokePermissions(rolesToRevoke, object, perm);
				}
			}

			Map<HibRole, Set<InternalPermission>> permissions = roleDao.getPermissions(allRoles, object);
			permissions.values().removeIf(Set::isEmpty);

			ObjectPermissionResponse response = new ObjectPermissionResponse();
			permissions.entrySet().forEach(entry -> {
				RoleReference role = entry.getKey().transformToReference();
				entry.getValue().forEach(perm -> response.add(role, perm.getRestPerm()));
			});
			response.setOthers(object.hasPublishPermissions());

			return response;
		}, model -> ac.send(model, OK));
	}
}
