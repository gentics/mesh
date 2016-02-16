package com.gentics.mesh.core.data.schema.handler;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.Microschema;

@Component
public class MicroschemaComparator extends AbstractFieldSchemaContainerComparator<Microschema> {

	private static MicroschemaComparator instance;

	@PostConstruct
	public void setup() {
		MicroschemaComparator.instance = this;
	}

	public static MicroschemaComparator getIntance() {
		return instance;
	}

}
