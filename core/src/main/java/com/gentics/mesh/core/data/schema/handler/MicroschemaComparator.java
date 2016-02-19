package com.gentics.mesh.core.data.schema.handler;

import java.io.IOException;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

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

	@Override
	public List<SchemaChangeModel> diff(Microschema containerA, Microschema containerB) throws IOException {
		return super.diff(containerA, containerB, Microschema.class);
	}

}
