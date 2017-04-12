package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * A user reference is a basic rest model POJO that contains a reference to a user in form of the firstname/lastname and the user uuid.
 */
public class UserReference implements RestModel {

	@JsonPropertyDescription("Firstname of the user")
	private String firstName;

	@JsonPropertyDescription("Lastname of the user")
	private String lastName;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Uuid of the user")
	private String uuid;

	/**
	 * Return the firstname of the user.
	 * 
	 * @return
	 */
	public String getFirstName() {
		return firstName;
	}

	/**
	 * Set the firstname of the user.
	 * 
	 * @param firstName
	 * @return Fluent API
	 */
	public UserReference setFirstName(String firstName) {
		this.firstName = firstName;
		return this;
	}

	/**
	 * Get the lastname of the user.
	 * 
	 * @return
	 */
	public String getLastName() {
		return lastName;
	}

	/**
	 * Set the lastname of the user.
	 * 
	 * @param lastName
	 * @return
	 */
	public UserReference setLastName(String lastName) {
		this.lastName = lastName;
		return this;
	}

	/**
	 * Return the uuid of the user.
	 * 
	 * @return
	 */
	public String getUuid() {
		return uuid;
	}

	/**
	 * Set the uuid of the user.
	 * 
	 * @param uuid
	 * @return
	 */
	public UserReference setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}

}
