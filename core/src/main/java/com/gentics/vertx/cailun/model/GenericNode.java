package com.gentics.vertx.cailun.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import org.springframework.data.neo4j.annotation.NodeEntity;

@NodeEntity
@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
public class GenericNode extends AbstractPersistable {

	private static final long serialVersionUID = -7525642021064006664L;

}
