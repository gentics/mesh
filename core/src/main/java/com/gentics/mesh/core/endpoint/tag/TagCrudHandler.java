package com.gentics.mesh.core.endpoint.tag;

import static com.gentics.mesh.core.action.DAOActionContext.context;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.TagDAOActions;
import com.gentics.mesh.core.action.TagFamilyDAOActions;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.TagDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.PageTransformer;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.ObjectPermissionGrantRequest;
import com.gentics.mesh.core.rest.common.ObjectPermissionResponse;
import com.gentics.mesh.core.rest.common.ObjectPermissionRevokeRequest;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.core.verticle.handler.WriteLock;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.NodeParameters;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.util.ResultInfo;

/**
 * Main CRUD handler
 */
public class TagCrudHandler extends AbstractHandler {

	private final HandlerUtilities utils;
	private final MeshOptions options;
	private final WriteLock globalLock;
	private final TagDAOActions tagActions;
	private final TagFamilyDAOActions tagFamilyActions;
	private final PageTransformer pageTransformer;

	@Inject
	public TagCrudHandler(MeshOptions options, HandlerUtilities utils, WriteLock writeLock, TagDAOActions tagActions,
		TagFamilyDAOActions tagFamilyActions, PageTransformer pageTransformer) {
		this.options = options;
		this.utils = utils;
		this.globalLock = writeLock;
		this.tagActions = tagActions;
		this.tagFamilyActions = tagFamilyActions;
		this.pageTransformer = pageTransformer;
	}

	/**
	 * Add the handler that returns a node list for a specified tag.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tag's parent tag family
	 * @param tagUuid
	 *            Uuid of the tag
	 */
	public void handleTaggedNodesList(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, tx -> {
				TagDao tagDao = tx.tagDao();
				PagingParameters pagingParams = ac.getPagingParameters();
				NodeParameters nodeParams = ac.getNodeParameters();
				HibTagFamily tagFamily = tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
				HibTag tag = tagActions.loadByUuid(context(tx, ac, tagFamily), tagUuid, READ_PERM, true);
				Page<? extends HibNode> page = tagDao.findTaggedNodes(tag, ac.getUser(), tx.getBranch(ac),
					nodeParams.getLanguageList(options),
					ContainerType.forVersion(ac.getVersioningParameters().getVersion()), pagingParams);
				return pageTransformer.transformToRestSync(page, ac, 0);
			}, model -> ac.send(model, OK));
		}
	}

	/**
	 * Read paged list of tags.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 */
	public void handleReadTagList(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tx.tagFamilyActions().loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.readElementList(ac, tagFamilyLoader, tagActions);
	}

	/**
	 * Handle a tag create request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily in which the tag should be created
	 */
	public void handleCreate(InternalActionContext ac, String tagFamilyUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");

		try (WriteLock lock = globalLock.lock(ac)) {
			utils.syncTx(ac, (batch, tx) -> {
				TagDao tagDao = tx.tagDao();

				HibTag tag = tagActions.create(tx, ac, batch, null);
				TagResponse model = tagDao.transformToRestSync(tag, ac, 0);
				String path = tagDao.getAPIPath(tag, ac);
				ResultInfo resultInfo = new ResultInfo(model);
				resultInfo.setProperty("path", path);
				ResultInfo info = resultInfo;

				ac.setLocation(info.getProperty("path"));
				return info.getModel();
			}, model -> ac.send(model, CREATED));
		}

	}

	/**
	 * Handle a tag delete request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            The tags tagfamily uuid
	 * @param tagUuid
	 *            Uuid of the tag which should be deleted
	 */
	public void handleUpdate(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tx.tagFamilyActions().loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.createOrUpdateElement(ac, tagFamilyLoader, tagUuid, tagActions);
	}

	/**
	 * Handle a tag read request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily to which the tag belongs
	 * @param tagUuid
	 *            Uuid of the tag which should be read
	 */
	public void handleRead(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tx.tagFamilyActions().loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.readElement(ac, tagFamilyLoader, tagUuid, tagActions, READ_PERM);

	}

	/**
	 * Handle a tag delete request.
	 * 
	 * @param ac
	 *            Action Context
	 * @param tagFamilyUuid
	 *            Uuid of the tagfamily to which the tag belongs
	 * @param tagUuid
	 *            Uuid of the tag which should be deleted
	 */
	public void handleDelete(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		Function<Tx, Object> tagFamilyLoader = tx -> {
			return tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
		};
		utils.deleteElement(ac, tagFamilyLoader, tagActions, tagUuid);

	}

	/**
	 * Handle request to read the permissions for all roles
	 * @param ac action context
	 * @param tagFamilyUuid Uuid of the tag family
	 * @param tagUuid Uuid of the tag
	 */
	public void handleReadPermissions(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			HibTagFamily tagFamily = tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
			HibTag tag = tagActions.loadByUuid(context(tx, ac, tagFamily), tagUuid, READ_PERM, true);

			Set<HibRole> roles = roleDao.findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream().collect(Collectors.toSet());

			Map<HibRole, Set<InternalPermission>> permissions = roleDao.getPermissions(roles, tag);
			permissions.values().removeIf(Set::isEmpty);

			ObjectPermissionResponse response = new ObjectPermissionResponse();
			permissions.entrySet().forEach(entry -> {
				RoleReference role = entry.getKey().transformToReference();
				entry.getValue().forEach(perm -> response.add(role, perm.getRestPerm()));
			});
			response.setOthers(tag.hasPublishPermissions());

			return response;
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle request to grant permissions on sets of roles
	 * @param ac action context
	 * @param tagFamilyUuid Uuid of the tag family
	 * @param tagUuid Uuid of the tag
	 */
	public void handleGrantPermissions(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		ObjectPermissionGrantRequest update = ac.fromJson(ObjectPermissionGrantRequest.class);
		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibUser requestUser = ac.getUser();
			HibTagFamily tagFamily = tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
			HibTag tag = tagActions.loadByUuid(context(tx, ac, tagFamily), tagUuid, READ_PERM, true);

			Set<HibRole> allRoles = roleDao.findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream().collect(Collectors.toSet());
			Map<String, HibRole> allRolesByUuid = allRoles.stream().collect(Collectors.toMap(HibRole::getUuid, Function.identity()));
			Map<String, HibRole> allRolesByName = allRoles.stream().collect(Collectors.toMap(HibRole::getName, Function.identity()));

			InternalPermission[] possiblePermissions = InternalPermission.basicPermissions();

			for (InternalPermission perm : possiblePermissions) {
				Set<RoleReference> roleRefsToSet = update.get(perm.getRestPerm());
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

					roleDao.grantPermissions(rolesToSet, tag, false, perm);

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
							roleDao.revokePermissions(rolesToRevoke, tag, perm);
						}
					}
				}
			}

			Map<HibRole, Set<InternalPermission>> permissions = roleDao.getPermissions(allRoles, tag);
			permissions.values().removeIf(Set::isEmpty);

			ObjectPermissionResponse response = new ObjectPermissionResponse();
			permissions.entrySet().forEach(entry -> {
				RoleReference role = entry.getKey().transformToReference();
				entry.getValue().forEach(perm -> response.add(role, perm.getRestPerm()));
			});
			response.setOthers(false);

			return response;
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle request to revoke permissions on sets of roles
	 * @param ac action context
	 * @param tagFamilyUuid Uuid of the tag family
	 * @param tagUuid Uuid of the tag
	 */
	public void handleRevokePermissions(InternalActionContext ac, String tagFamilyUuid, String tagUuid) {
		validateParameter(tagFamilyUuid, "tagFamilyUuid");
		validateParameter(tagUuid, "tagUuid");

		ObjectPermissionRevokeRequest update = ac.fromJson(ObjectPermissionRevokeRequest.class);
		utils.syncTx(ac, tx -> {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibUser requestUser = ac.getUser();
			HibTagFamily tagFamily = tagFamilyActions.loadByUuid(context(tx, ac), tagFamilyUuid, READ_PERM, true);
			HibTag tag = tagActions.loadByUuid(context(tx, ac, tagFamily), tagUuid, READ_PERM, true);

			Set<HibRole> allRoles = roleDao.findAll(ac, new PagingParametersImpl().setPerPage(Long.MAX_VALUE)).stream().collect(Collectors.toSet());
			Map<String, HibRole> allRolesByUuid = allRoles.stream().collect(Collectors.toMap(HibRole::getUuid, Function.identity()));
			Map<String, HibRole> allRolesByName = allRoles.stream().collect(Collectors.toMap(HibRole::getName, Function.identity()));

			InternalPermission[] possiblePermissions = InternalPermission.basicPermissions();

			for (InternalPermission perm : possiblePermissions) {
				Set<RoleReference> roleRefsToRevoke = update.get(perm.getRestPerm());
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

					roleDao.revokePermissions(rolesToRevoke, tag, perm);
				}
			}

			Map<HibRole, Set<InternalPermission>> permissions = roleDao.getPermissions(allRoles, tag);
			permissions.values().removeIf(Set::isEmpty);

			ObjectPermissionResponse response = new ObjectPermissionResponse();
			permissions.entrySet().forEach(entry -> {
				RoleReference role = entry.getKey().transformToReference();
				entry.getValue().forEach(perm -> response.add(role, perm.getRestPerm()));
			});
			response.setOthers(false);

			return response;
		}, model -> ac.send(model, OK));
	}
}
