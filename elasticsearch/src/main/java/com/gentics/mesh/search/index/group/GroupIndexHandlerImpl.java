package com.gentics.mesh.search.index.group;

import static com.gentics.mesh.util.PreparationUtil.prepareData;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.tuple.Pair;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import io.reactivex.Flowable;

/**
 * Handler for the elastic search group index.
 */
@Singleton
public class GroupIndexHandlerImpl extends AbstractIndexHandler<HibGroup> implements GroupIndexHandler {

	protected final GroupTransformer transformer;

	protected final GroupMappingProvider mappingProvider;

	@Inject
	public GroupIndexHandlerImpl(SearchProvider searchProvider, Database db, MeshHelper helper, MeshOptions options, Compliance compliance,
		SyncMetersFactory syncMetersFactory, BucketManager bucketManager, GroupTransformer transformer, GroupMappingProvider mappingProvider) {
		super(searchProvider, db, helper, options, syncMetersFactory, bucketManager, compliance);
		this.transformer = transformer;
		this.mappingProvider = mappingProvider;
	}

	@Override
	public String getType() {
		return "group";
	}

	@Override
	public Class<HibGroup> getElementClass() {
		return HibGroup.class;
	}

	@Override
	public long getTotalCountFromGraph() {
		return db.tx(tx -> {
			return tx.groupDao().count();
		});
	}

	@Override
	public GroupTransformer getTransformer() {
		return transformer;
	}

	@Override
	public GroupMappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, Optional<IndexInfo>> getIndices() {
		String indexName = HibGroup.composeIndexName();
		IndexInfo info = new IndexInfo(indexName, null, getMappingProvider().getMapping(), "group");
		return Collections.singletonMap(indexName, Optional.of(info));
	}

	@Override
	public Set<String> getIndicesForSearch(InternalActionContext ac) {
		return Collections.singleton(HibGroup.composeIndexName());
	}

	@Override
	public Function<String, HibGroup> elementLoader() {
		return (uuid) -> Tx.get().groupDao().findByUuid(uuid);
	}

	@Override
	public Function<Collection<String>, Stream<Pair<String, HibGroup>>> elementsLoader() {
		return (uuids) -> Tx.get().groupDao().findByUuids(uuids);
	}

	@Override
	public Collection<? extends HibGroup> loadAllElements(Bucket bucket, DataHolderContext dhc) {
		GroupDao groupDao = Tx.get().groupDao();
		RoleDao roleDao = Tx.get().roleDao();
		List<HibGroup> groupsInBucket = new ArrayList<>(groupDao.findAll(bucket).list());

		prepareData(groupsInBucket, dhc, "group", "permissions",
				elements -> roleDao.getRoleUuidsForPerm(elements, InternalPermission.READ_PERM));

		return groupsInBucket;
	}

	@Override
	public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
		return diffAndSync(HibGroup.composeIndexName(), null, indexPattern);
	}

	@Override
	public Set<String> filterUnknownIndices(Set<String> indices) {
		return filterIndicesByType(indices, HibGroup.composeIndexName());
	}

}
