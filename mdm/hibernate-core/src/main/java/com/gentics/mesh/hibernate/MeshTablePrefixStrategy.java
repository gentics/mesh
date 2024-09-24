package com.gentics.mesh.hibernate;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategyStandardImpl;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * Adds the "mesh_" prefix to table names. This prevents using reserved keywords as names (ex. user)
 * See https://stackoverflow.com/questions/22256124/cannot-create-a-database-table-named-user-in-postgresql
 *
 * This class must be referenced in the persistence.xml or in the overridden properties.
 * @see com.gentics.mesh.database.DefaultSQLDatabase
 */
public class MeshTablePrefixStrategy extends PhysicalNamingStrategyStandardImpl {
	private static final long serialVersionUID = 4258983458733000783L;

	// TODO HIB consider making this configurable
	public static final String TABLE_NAME_PREFIX = "mesh_";
	public static final String CONTENT_TABLE_NAME_PREFIX = "content_";

	@Override
	public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment context) {
		Identifier newIdentifier = new Identifier(TABLE_NAME_PREFIX + name.getText().toLowerCase(), name.isQuoted());
		return super.toPhysicalTableName(newIdentifier, context);
	}

	@Override
	public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment context) {
		Identifier newIdentifier = new Identifier(name.getText().toLowerCase(), name.isQuoted());
		return super.toPhysicalColumnName(newIdentifier, context);
	}
}
