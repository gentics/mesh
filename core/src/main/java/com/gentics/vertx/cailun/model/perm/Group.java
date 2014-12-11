package com.gentics.vertx.cailun.model.perm;

import org.springframework.data.neo4j.annotation.NodeEntity;

import com.gentics.vertx.cailun.model.AbstractPersistable;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
@EqualsAndHashCode(doNotUseGetters = true, callSuper = false)
@NodeEntity
public class Group extends AbstractPersistable {

	private static final long serialVersionUID = -6423363555276535419L;

}
