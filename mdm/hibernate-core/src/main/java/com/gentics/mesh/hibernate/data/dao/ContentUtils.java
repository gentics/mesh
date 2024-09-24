package com.gentics.mesh.hibernate.data.dao;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.HibQueryFieldMapper;
import com.gentics.mesh.hibernate.data.domain.AbstractHibFieldSchemaVersion;
import com.gentics.mesh.hibernate.data.domain.HibBooleanListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibDateListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibHtmlListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.domain.HibStringListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibMicronodeFieldImpl;
import com.gentics.mesh.hibernate.data.node.field.impl.HibNodeListFieldImpl;
import com.gentics.mesh.util.UUIDUtil;

import jakarta.persistence.criteria.JoinType;

/**
 * Utils for micronode & micronode content.
 * 
 * @author plyhun
 *
 */
public final class ContentUtils {

	private ContentUtils() {}

	/**
	 * Get micronode content field mapper.
	 * 
	 * @return
	 */
	public static final HibQueryFieldMapper micronodeQueryFieldMapper() {
		return new HibQueryFieldMapper() {

			@Override
			public String[] getHibernateEntityName(Object... args) {
				List<String> versionTables = Arrays.stream(args).filter(AbstractHibFieldSchemaVersion.class::isInstance).map(AbstractHibFieldSchemaVersion.class::cast).map(version -> MeshTablePrefixStrategy.CONTENT_TABLE_NAME_PREFIX + UUIDUtil.toShortUuid(version.getUuid())).collect(Collectors.toList());
				if (versionTables.size() > 0) {
					return versionTables.toArray(new String[versionTables.size()]);
				} else {
					return new String[] {HibernateTx.get().data().getDatabaseConnector().maybeGetDatabaseEntityName(HibNodeFieldContainerEdgeImpl.class).get()};
				}
			}

			@Override
			public boolean isContent() {
				return true;
			}

			@Override
			public String mapGraphQlFilterFieldName(String gqlName) {
				switch (gqlName) {
				case "microschema": return "microschemaVersion_dbUuid";
				}
				return gqlName;
			}
		};
	}

	public static final HibQueryFieldMapper listFieldQueryMapper() {
		return new HibQueryFieldMapper() {
			
			@Override
			public String[] getHibernateEntityName(Object... args) {
				DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
				List<String> tables = Arrays.stream(args)
						.filter(FieldTypes.class::isInstance)
						.map(FieldTypes.class::cast)
						.map(etype -> {
							switch (etype) {
							case STRING: return Stream.of(HibStringListFieldEdgeImpl.class);
							case BOOLEAN: return Stream.of(HibBooleanListFieldEdgeImpl.class);
							case DATE: return Stream.of(HibDateListFieldEdgeImpl.class);
							case HTML: return Stream.of(HibHtmlListFieldEdgeImpl.class);
							case MICRONODE: return Stream.of(HibMicronodeListFieldEdgeImpl.class, HibMicronodeFieldImpl.class);
							case NODE: return Stream.of(HibNodeListFieldEdgeImpl.class, HibNodeListFieldImpl.class);
							default: return Stream.empty();
							}
						}).flatMap(Function.identity())
						.map(Class.class::cast)
						.map(dc::maybeGetDatabaseEntityName)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(Collectors.toList());
				if (tables.size() > 0) {
					return tables.toArray(new String[tables.size()]);
				} else {
					return new String[] {};//AbstractHibListFieldEdgeImpl
				}				
			}

			@Override
			public String mapGraphQlFilterFieldName(String gqlName) {
				switch (gqlName) {
				case "value": return "valueOrUuid";
				}
				return HibQueryFieldMapper.super.mapGraphQlFilterFieldName(gqlName);
			}
		};
	}

	public static final HibQueryFieldMapper referenceFieldQueryMapper() {
		return new HibQueryFieldMapper() {
			
			@Override
			public String[] getHibernateEntityName(Object... arg) {
				DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
				return new String[] {
						dc.maybeGetDatabaseEntityName(HibNodeListFieldEdgeImpl.class).get(), 
						dc.maybeGetDatabaseEntityName(HibNodeFieldEdgeImpl.class).get()
					};
			}

			@Override
			public String mapGraphQlFilterFieldName(String gqlName) {
				switch (gqlName) {
				case "value": return "valueOrUuid";
				case "count": return "COUNT(REFERENCES)";
				}
				return HibQueryFieldMapper.super.mapGraphQlFilterFieldName(gqlName);
			}
		};
	}

	public static final String[] getReferenceCountComparisonLHS() {
		return new String[] {
				"(select count(1) from mesh_nodefieldref nodefieldref_ where nodefieldref_.valueOrUuid = node_.dbuuid)", 
				"(select count(1) from mesh_nodelistitem nodelistitem_ where nodelistitem_.valueOrUuid = node_.dbuuid)"
			};
	}

	public static final JoinType getJoinType(String hibernateEntityName) {
		switch (hibernateEntityName) {
		case "nodefieldref":
		case "nodelistitem":
			return JoinType.LEFT;
		default:
			return JoinType.INNER;
		}
	}
}
