package com.gentics.mesh.plugin.forms.service;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.plugin.forms.SchemaInitializer;

/**
 * Service implementation for the migration of binary form fields.
 * Older versions of the forms plugin stored binary data directly in binary fields of the form data nodes.
 * Newer versions will store the binary data as nodes of schema {@link SchemaInitializer#MULTIBINARY_ITEM_SCHEMA_NAME}
 */
@Singleton
public class BinaryDataMigrationService {
	@Inject
	public BinaryDataMigrationService() {
	}

}
