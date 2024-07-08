package com.gentics.mesh.enterprise.hibernate.dialect;

import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.LONGNVARCHAR;
import static org.hibernate.type.SqlTypes.LONGVARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.VARCHAR;

import java.sql.Types;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.ColumnAliasExtractor;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import com.gentics.mesh.hibernate.TableAwareColumnAliasExtractor;

/**
 * An extension to the latest SQL Server dialect, making use of Unicode-aware
 * multibyte character types by default.
 * 
 * @author plyhun
 *
 */
public class SqlServerUnicodeDialect extends SQLServerDialect {

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( CHAR, "nchar(1)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONGVARCHAR, "nvarchar(max)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( VARCHAR, "nvarchar(max)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( CLOB, "nvarchar(max)", this ) );

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( NCHAR, "nchar(1)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( LONGNVARCHAR, "nvarchar(max)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( NVARCHAR, "nvarchar(max)", this ) );
		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( NCLOB, "nvarchar(max)", this ) );
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
            String columnTypeName,
            int jdbcTypeCode,
            int precision,
            int scale,
            JdbcTypeRegistry jdbcTypeRegistry) {
        
        switch ( jdbcTypeCode ) {
            case Types.NCHAR:
                jdbcTypeCode = StandardBasicTypes.CHARACTER.getSqlTypeCode();
                break;
            case Types.LONGNVARCHAR:
            	jdbcTypeCode = StandardBasicTypes.TEXT.getSqlTypeCode();
                break;
            case Types.NVARCHAR:
            	jdbcTypeCode = StandardBasicTypes.STRING.getSqlTypeCode();
                break;
            case Types.NCLOB:
                jdbcTypeCode = StandardBasicTypes.CLOB.getSqlTypeCode();
                break;
        }
        return super.resolveSqlTypeDescriptor( columnTypeName, jdbcTypeCode, precision, scale, jdbcTypeRegistry );
    }

	@Override
	public ColumnAliasExtractor getColumnAliasExtractor() {
		return TableAwareColumnAliasExtractor.INSTANCE;
	}
}