package com.gentics.mesh.enterprise.database.connector;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.enterprise.hibernate.dialect.OracleLongTextDialect;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.handler.RuntimeServiceRegistry;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.hibernate.data.domain.AbstractHibListFieldEdgeImpl;
import com.gentics.mesh.hibernate.data.node.field.HibListFieldEdge;
import com.gentics.mesh.hibernate.util.HibernateUtil;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

/**
 * Oracle database connector for Gentics Mesh
 */
public class OracleConnector extends AbstractEnterpriseDatabaseConnector {

	public OracleConnector(HibernateMeshOptions options, RuntimeServiceRegistry runtimeServiceRegistry) {
		super(options, runtimeServiceRegistry);
	}

	@Override
	public String getConnectionUrl() {
		return "jdbc:oracle:thin:@" + options.getStorageOptions().getDatabaseAddress() + "/" + options.getStorageOptions().getDatabaseName();
	}

	@Override
	public String installCount(String sqlQuery) {
		return "SELECT COUNT(1) FROM ( " + sqlQuery + " ) ";
	}

	@Override
	public String installListContentColumn(String listItemType, String listAlias, String operator, String paramName) {
		if ("string".equalsIgnoreCase(listItemType) || "html".equalsIgnoreCase(listItemType)) {
			return unblobStringContentColumn(listAlias + "." + renderNonContentColumn("valueOrUuid"), false, false) + " " + operator + " " + ":" + paramName;
		} else {
			return super.installListContentColumn(listItemType, listAlias, operator, paramName);
		}
	}

	@Override
	public String installStringContentColumn(String columnName, boolean makeAlias, boolean customAlias) {
		if (columnName.endsWith("-string\"") || columnName.endsWith("-html\"")) {
			return unblobStringContentColumn(columnName, makeAlias, customAlias);
		} else {
			return super.installStringContentColumn(columnName, makeAlias, customAlias);
		}
	}

	@Override
	public String findSortAlias(String columnDef) {
		return columnDef.indexOf(" AS ") > -1 ? columnDef.substring(columnDef.indexOf(" AS ") + 4) : super.findSortAlias(columnDef);
	}

	@Override
	public String makeSortDefinition(String column, Optional<String> customSortAlias) {
		return customSortAlias.orElseGet(() -> {
				String[] parts = column.split(" AS ");
				if (parts.length == 2) {
					return " MAX(" + parts[0] + ") AS " + makeSortAlias(parts[1]);
				} else {
					return " MAX(" + column + ") AS " + makeSortAlias(column);
				}
		});
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 1000;
	}

	@Override
	public String renderNonContentColumn(String column) {
		return column;
	}

	@Override
	public String getDummyComparison(Map<String, Object> params, boolean mustPass) {
		Boolean value = Boolean.valueOf(mustPass);
		String trueParam = HibernateUtil.makeParamName(value);
		params.put(trueParam, value);
		return " AND :" + trueParam + " ";
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public <V, T extends HibListFieldEdge<V>> Query makeListItemsMultiInsertionQuery(List<T> slice, EntityManager em, long startAt) {
		
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT ALL ");
		sb.append(IntStream.range(0, slice.size()).mapToObj(index -> String.format(
				"INTO %s%s (containertype, containeruuid, containerversionuuid, fieldkey, valueoruuid, itemindex, listuuid, dbuuid) VALUES (:containertype_%d, :containeruuid_%d, :containerversionuuid_%d, :fieldkey_%d, :valueoruuid_%d, :itemindex_%d, :listuuid_%d, :dbuuid_%d)", 
				MeshTablePrefixStrategy.TABLE_NAME_PREFIX, AbstractHibListFieldEdgeImpl.getEntityTableName((Class) slice.get(0).getClass()), index, index, index, index, index, index, index, index)).collect(Collectors.joining(" ")));
		sb.append(" SELECT 1 FROM DUAL");
		Query query = em.createNativeQuery(sb.toString());
		IntStream.range(0, slice.size()).forEach(index -> {
			T item = slice.get(index);
			query.setParameter("containertype_" + index, item.getContainerType().name());
			query.setParameter("containeruuid_" + index, item.getContainerUuid());
			query.setParameter("containerversionuuid_" + index, item.getContainerVersionUuid());
			query.setParameter("fieldkey_" + index, item.getFieldName());
			query.setParameter("valueoruuid_" + index, item.getValueOrUuid());
			query.setParameter("itemindex_" + index, startAt + index);
			query.setParameter("listuuid_" + index, item.getListUuid());
			query.setParameter("dbuuid_" + index, item.getDbUuid());
		});
		return query;
	}

	protected String unblobStringContentColumn(String columnName, boolean makeAlias, boolean customAlias) {
		return " TO_CHAR(" + columnName + ") " + (makeAlias ? ("AS " + columnName.substring(columnName.indexOf(".") + 1, columnName.length() - 1) + (customAlias ? Integer.toHexString(columnName.hashCode()) : StringUtils.EMPTY) + "\"") : StringUtils.EMPTY);
	}

	@Override
	protected String getDefaultDriverClassName() {
		return "oracle.jdbc.driver.OracleDriver";
	}

	@Override
	protected String getDefaultDialectClassName() {
		return OracleLongTextDialect.class.getCanonicalName();
	}
}
