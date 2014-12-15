package com.gentics.vertx.cailun.model.perm;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.vertx.cailun.model.AbstractPersistable;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class User extends AbstractPersistable {

	private String lastname;

	private String firstname;

	private String emailAddress;

	private static final long serialVersionUID = -8707906688270506022L;

	public User(String firstname, String lastname, String emailAddress) {
		this.firstname = firstname;
		this.lastname = lastname;
		this.emailAddress = emailAddress;
	}

}
