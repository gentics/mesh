package com.gentics.mesh.core.data;

import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.NodeMigrationActionContext;
import com.gentics.mesh.core.TypeInfo;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibCreatorTracking;
import com.gentics.mesh.core.data.user.HibEditorTracking;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.GenericRestResponse;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.event.role.PermissionChangedProjectElementEventModel;
import com.gentics.mesh.parameter.GenericParameters;
import com.gentics.mesh.parameter.value.FieldsSet;
import com.gentics.mesh.util.ETag;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * A core element is a public element which is also usually accessible via REST.
 */
public interface HibCoreElement<T extends RestModel> extends HibTransformableElement<T> {
	
	static final Logger log = LoggerFactory.getLogger(HibCoreElement.class);

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
	 * Method which is being invoked once the permissions on the element have been updated.
	 * 
	 * @param role
	 * @return
	 */
	default PermissionChangedEventModelImpl onPermissionChanged(HibRole role) {
		PermissionChangedEventModelImpl model = new PermissionChangedEventModelImpl();
		fillPermissionChanged(model, role);
		return model;
	}

	/**
	 * Add the common permission information to the model.
	 * 
	 * @param model
	 * @param role
	 */
	default void fillPermissionChanged(PermissionChangedEventModelImpl model, HibRole role) {
		model.setEvent(ROLE_PERMISSIONS_CHANGED);
		model.setRole(role.transformToReference());
		model.setType(getTypeInfo().getType());
		model.setUuid(getUuid());
		if (this instanceof HibNamedElement) {
			String name = ((HibNamedElement) this).getName();
			model.setName(name);
		}
		if (this instanceof HibProjectElement) {
			HibProject project = ((HibProjectElement) this).getProject();
			if (project != null) {
				if (model instanceof PermissionChangedProjectElementEventModel) {
					((PermissionChangedProjectElementEventModel) model).setProject(project.transformToReference());
				}
			} else {
				log.warn("The project for element {" + getUuid() + "} could not be found.");
			}
		}
	}

	/**
	 * Add common fields to the given rest model object. The method will add common files like creator, editor, uuid, permissions, edited, created.
	 * 
	 * @param model
	 * @param fields
	 *            Set of fields which should be included. All fields will be included if no selective fields have been specified.
	 * @param ac
	 */
	default void fillCommonRestFields(InternalActionContext ac, FieldsSet fields, GenericRestResponse model) {
		if (fields.has("uuid")) {
			model.setUuid(getUuid());
		}

		if (this instanceof HibEditorTracking) {
			HibEditorTracking edited = (HibEditorTracking) this;

			if (fields.has("editor")) {
				HibUser editor = edited.getEditor();
				if (editor != null) {
					model.setEditor(editor.transformToReference());
				} else {
					log.error("The object {" + getClass().getSimpleName() + "} with uuid {" + getUuid() + "} has no editor. Omitting editor field");
				}
			}

			if (fields.has("edited")) {
				String date = edited.getLastEditedDate();
				model.setEdited(date);
			}
		}

		if (this instanceof HibCreatorTracking) {
			HibCreatorTracking created = (HibCreatorTracking) this;
			if (fields.has("creator")) {
				HibUser creator = created.getCreator();
				if (creator != null) {
					model.setCreator(creator.transformToReference());
				}
			}
			if (fields.has("created")) {
				String date = created.getCreationDate();
				model.setCreated(date);
			}
		}

		if (fields.has("perms")) {
			// When this is a node migration, do not set user permissions
			if (!(ac instanceof NodeMigrationActionContext)) {
				PermissionInfo permissionInfo = Tx.get().userDao().getPermissionInfo(ac.getUser(), this);
				model.setPermissions(permissionInfo);
			}
		}
	}

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

	/**
	 * Method which is being invoked once the element has been updated.
	 * 
	 * @return Created event
	 */
	default MeshElementEventModel onUpdated() {
		return createEvent(getTypeInfo().getOnUpdated());
	}

	/**
	 * Method which is being invoked once the element has been created.
	 */
	default MeshElementEventModel onCreated() {
		return createEvent(getTypeInfo().getOnCreated());
	}

	/**
	 * Method which is being invoked once the element has been deleted.
	 * 
	 * @return Created event
	 */
	default MeshElementEventModel onDeleted() {
		return createEvent(getTypeInfo().getOnDeleted());
	}

	default MeshElementEventModel createEvent(MeshEvent event) {
		MeshElementEventModel model = new MeshElementEventModelImpl();
		model.setEvent(event);
		fillEventInfo(model);
		return model;
	}

	default void fillEventInfo(MeshElementEventModel model) {
		if (this instanceof HibNamedElement) {
			model.setName(((HibNamedElement) this).getName());
		}
		model.setOrigin(Tx.get().data().options().getNodeName());
		model.setUuid(getUuid());
	}
}
