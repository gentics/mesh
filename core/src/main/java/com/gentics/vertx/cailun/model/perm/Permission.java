package com.gentics.vertx.cailun.model.perm;

import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.RelationshipEntity;
import org.springframework.data.neo4j.annotation.StartNode;

import com.gentics.vertx.cailun.model.AbstractPersistable;

/**
 * The permission object is an element that is used to form the ACL domain in the graph.
 * 
 * @author johannes2
 *
 */
@RelationshipEntity
//@Data
//@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NoArgsConstructor
public class Permission extends AbstractPersistable {

	private static final long serialVersionUID = 1564260534291371364L;


	@Fetch
	@StartNode
	private Group group;

//	@Fetch
//	@EndNode
//	private GenericNode object;

//	public Permission(Group group, GenericNode object) {
//		this.group = group;
//		this.object = object;
//	}

	public Group getGroup() {
		return group;
	}

	public void setGroup(Group group) {
		this.group = group;
	}

//	public GenericNode getObject() {
//		return object;
//	}
//
//	public void setObject(GenericNode object) {
//		this.object = object;
//	}
}
