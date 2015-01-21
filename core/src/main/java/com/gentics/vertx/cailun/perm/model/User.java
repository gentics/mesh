package com.gentics.vertx.cailun.perm.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.vertx.cailun.base.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class User extends AbstractPersistable {

	private String lastname;

	private String firstname;

	private String username;

	private String emailAddress;

	private String passwordHash;

	private static final long serialVersionUID = -8707906688270506022L;

	public User(String username) {
		this.username = username;
	}

	public String toString() {
		return username;
	}

}
