package com.gentics.mesh.enterprise.hibernate.dialect;

import static org.hibernate.type.SqlTypes.JSON;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.ColumnAliasExtractor;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import com.gentics.mesh.hibernate.TableAwareColumnAliasExtractor;

/**
 * PostgreSQL returns the raw (and possibly duplicated) column labels, so here we fix them to be more unique.
 */
public class PostgresDuplicationAwareDialect extends PostgreSQLDialect {

	public PostgresDuplicationAwareDialect() {
		super();
	}

	public PostgresDuplicationAwareDialect(DialectResolutionInfo info) {
		super(info);
	}

	public PostgresDuplicationAwareDialect(DatabaseVersion version) {
		super(version);
	}

	@Override
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return TableAwareColumnAliasExtractor.INSTANCE;
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes(typeContributions, serviceRegistry);
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( JSON, "text", this ) );
	}
}
