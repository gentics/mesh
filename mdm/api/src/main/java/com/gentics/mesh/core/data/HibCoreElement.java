package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;

/**
 * A core element is a public element which is also usually accessible via REST.
 */
public interface HibCoreElement<T extends RestModel> extends HibTransformableElement<T> {

	/**
	 * Method which is being invoked once the element has been created.
	 */
	MeshElementEventModel onCreated();

	/**
	 * Method which is being invoked once the element has been updated.
	 * 
	 * @return Created event
	 */
	MeshElementEventModel onUpdated();

	/**
	 * Method which is being invoked once the element has been deleted.
	 * 
	 * @return Created event
	 */
	MeshElementEventModel onDeleted();

	/**
	 * Return the type info of the element.
	 * 
	 * @return
	 */
	TypeInfo getTypeInfo();

	/**
	 * This method provides the element specific etag. It needs to be individually implemented for all core element classes.
	 * 
	 * @param ac
	 * @return
	 */
	String getSubETag(InternalActionContext ac);

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 * 
	 * @param model
	 * @param fields
	 *            Set of fields which should be included. All fields will be included if no selective fields have been specified.
	 * @param ac
	 */
	void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model);

	@Override
	default String getETag(InternalActionContext ac) {
		UserDao userDao = Tx.get().userDao();
		RoleDao roleDao = Tx.get().roleDao();

		StringBuilder keyBuilder = new StringBuilder();
		keyBuilder.append(getUuid());
		keyBuilder.append("-");
		keyBuilder.append(userDao.getPermissionInfo(ac.getUser(), this).getHash());

		keyBuilder.append("fields:");
		GenericParameters generic = ac.getGenericParameters();
		FieldsSet fields = generic.getFields();
		fields.forEach(keyBuilder::append);

		/**
		 * permissions (&roleUuid query parameter aware)
		 * 
		 * Permissions can change and thus must be included in the etag computation in order to invalidate the etag once the permissions change.
		 */
		String roleUuid = ac.getRolePermissionParameters().getRoleUuid();
		if (!isEmpty(roleUuid)) {
			HibRole role = roleDao.loadObjectByUuid(ac, roleUuid, READ_PERM);
			if (role != null) {
				Set<InternalPermission> permSet = roleDao.getPermissions(role, this);
				Set<String> humanNames = new HashSet<>();
				for (InternalPermission permission : permSet) {
					humanNames.add(permission.getRestPerm().getName());
				}
				String[] names = humanNames.toArray(new String[humanNames.size()]);
				keyBuilder.append(Arrays.toString(names));
			}

		}

		// Add the type specific etag part
		keyBuilder.append(getSubETag(ac));
		return ETag.hash(keyBuilder.toString());
	}
}
