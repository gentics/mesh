package com.gentics.mesh.core.field;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.AbstractBasicDBTest;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;

public abstract class AbstractFieldTest extends AbstractBasicDBTest implements FieldTestcases{

	@Autowired
	protected ServerSchemaStorage schemaStorage;

}
