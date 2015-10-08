package com.gentics.mesh.core.rest.role;

/**
 * POJO for a role request model that is used for role creation.
 */
public class RoleCreateRequest extends RoleUpdateRequest {

	private String groupUuid;

	public RoleCreateRequest() {
	}

	//TODO We should get rid of these methods. It should be possible to create a role without specifying any group.
	/**
	 * Return the group uuid to which the role should be initially assigned.
	 * 
	 * @return
	 */
	public String getGroupUuid() {
		return groupUuid;
	}

	/**
	 * Set the group uuid.
	 * 
	 * @param groupUuid
	 */
	public void setGroupUuid(String groupUuid) {
		this.groupUuid = groupUuid;
	}

}
