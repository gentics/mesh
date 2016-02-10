package com.gentics.mesh.core.data.schema.handler;

import java.util.List;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
public class MicroschemaComparator extends AbstractFieldSchemaContainerComparator<Microschema> {

	private static final Logger log = LoggerFactory.getLogger(MicroschemaComparator.class);

	@Override
	public List<SchemaChangeModel> diff(Microschema containerA, Microschema containerB) {
		return super.diff(containerA, containerB);
	}

}
