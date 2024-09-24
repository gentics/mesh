package com.gentics.mesh.contentoperation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hibernate.query.ResultListTransformer;
import org.hibernate.query.TupleTransformer;

import com.gentics.mesh.hibernate.data.domain.AbstractHibDatabaseElement;
import com.gentics.mesh.hibernate.data.domain.HibUnmanagedFieldContainer;

/**
 * Transforms a list of tuples into a list of (micro)node field containers
 */
public class ContentResultTransformer<T extends HibUnmanagedFieldContainer<?, ?, ?, ?, ?>> implements ResultListTransformer<T>, TupleTransformer<T> {

	private final UUID versionId;
	private final Map<String, ContentColumn> columnByAlias;
	private final Supplier<T> constructor;
	private Map<UUID, T> fieldContainerByUuid = new HashMap<>();

	public ContentResultTransformer(UUID versionId, Map<String, ContentColumn> columnByAlias, Supplier<T> constructor) {
		this.versionId = versionId;
		this.columnByAlias = columnByAlias;
		this.constructor = constructor;
	}

	@Override
	public T transformTuple(Object[] tuple, String[] aliases) {
		Map<ContentColumn, Object> columnValueMap = columnValueMap(tuple, aliases);
		UUID dbUuid = (UUID) columnValueMap.get(CommonContentColumn.DB_UUID);
		T fieldContainer = fieldContainerByUuid.get(dbUuid);
		// handle deduplication
		if (fieldContainer == null) {
			fieldContainer = constructor.get();
			fieldContainer.setDbUuid(dbUuid);
			((AbstractHibDatabaseElement) fieldContainer).setDbVersion((Long) columnValueMap.get(CommonContentColumn.DB_VERSION));
			fieldContainer.setSchemaContainerVersionByUuid(versionId);
			fieldContainerByUuid.put(dbUuid, fieldContainer);
		}

		populateNonJoinedColumns(fieldContainer, columnValueMap);

		return fieldContainer;
	}

	private Map<ContentColumn, Object> columnValueMap(Object[] tuple, String[] aliases) {
		Map<ContentColumn, Object> columnValueMap = new HashMap<>();
		for (int i = 0; i < aliases.length; i++) {
			ContentColumn contentColumn = columnByAlias.get(aliases[i]);
			columnValueMap.put(contentColumn, tuple[i]);
		}
		return columnValueMap;
	}

	private void populateNonJoinedColumns(T fieldContainer, Map<ContentColumn, Object> columnValueMap) {
		columnValueMap.entrySet().forEach(kv -> {
			if (!(kv.getKey() instanceof JoinedContentColumn)) {
				fieldContainer.put(kv.getKey(), kv.getValue());
			}
		});
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public List transformList(List collection) {
		return (List) collection.stream().distinct().collect(Collectors.toList());
	}
}

