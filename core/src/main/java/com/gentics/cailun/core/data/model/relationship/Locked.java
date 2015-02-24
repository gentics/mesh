package com.gentics.cailun.core.data.model.relationship;

import org.springframework.data.neo4j.annotation.EndNode;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.model.generic.AbstractPersistable;
import com.gentics.cailun.core.data.model.generic.GenericNode;

@RelationshipEntity
public class Locked extends AbstractPersistable {

	// TODO locking time? neo4j transaction timestamp?

	private static final long serialVersionUID = 382199463514300873L;

	@StartNode
	private GenericNode lockedObject;

	@EndNode
	private User lockedBy;

	public boolean isValidLock() {
		// TODO verify that this lock is currently valid
		return false;
	}

}
