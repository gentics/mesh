package com.gentics.mesh.search.index.role;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.root.RootVertex;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.UpdateDocumentEntry;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.entry.AbstractIndexHandler;

/**
 * Handler for the elasticsearch role index.
 */
@Singleton
public class RoleIndexHandler extends AbstractIndexHandler<Role> {

	@Inject
	RoleTransformer transformer;

	@Inject
	RoleMappingProvider mappingProvider;

	@Inject
	public RoleIndexHandler(SearchProvider searchProvider, Database db, BootstrapInitializer boot, SearchQueue searchQueue) {
		super(searchProvider, db, boot, searchQueue);
	}

	@Override
	public Class<Role> getElementClass() {
		return Role.class;
	}

	@Override
	protected String composeDocumentIdFromEntry(UpdateDocumentEntry entry) {
		return Role.composeDocumentId(entry.getElementUuid());
	}

	@Override
	protected String composeIndexNameFromEntry(UpdateDocumentEntry entry) {
		return Role.composeIndexName();
	}

	@Override
	protected String composeIndexTypeFromEntry(UpdateDocumentEntry entry) {
		return Role.composeIndexType();
	}

	@Override
	public RoleTransformer getTransformer() {
		return transformer;
	}

	@Override
	protected MappingProvider getMappingProvider() {
		return mappingProvider;
	}

	@Override
	public Map<String, IndexInfo> getIndices() {
		String type = Role.TYPE;
		IndexInfo info = new IndexInfo(type, type, null, getMappingProvider().getMapping(type));
		return Collections.singletonMap(type, info);
	}

	@Override
	public Set<String> getSelectedIndices(InternalActionContext ac) {
		return Collections.singleton(Role.TYPE);
	}

	@Override
	public RootVertex<Role> getRootVertex() {
		return boot.meshRoot().getRoleRoot();
	}

}
