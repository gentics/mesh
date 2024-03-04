package com.gentics.mesh.core.data.root;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import com.gentics.graphqlfilter.filter.operation.FilterOperation;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Aggregation node for nodes.
 */
public interface NodeRoot extends RootVertex<Node> {

	/**
	 * Stream all nodes with content of a specific type.
	 * 
	 * @param ac
	 * @param perm
	 * @param maybeContainerType
	 * @param paging
	 * @param maybeFilter
	 * @return
	 */
	Stream<? extends Node> findAllStream(InternalActionContext ac, InternalPermission perm, PagingParameters paging, Optional<ContainerType> maybeContainerType, Optional<FilterOperation<?>> maybeFilter);

	@Override
	default Stream<? extends Node> findAllStream(InternalActionContext ac, InternalPermission permission,
			PagingParameters paging, Optional<FilterOperation<?>> maybeFilter) {
		return findAllStream(ac, permission, paging, Optional.empty(), maybeFilter);
	}

	/**
	 * Check if the requested languages being used by any node in the given root.
	 * 
	 * @param languageTags
	 * @param assignedLanguagesOnly pick only languages, known to this project.
	 * @return
	 */
	Set<String> findUsedLanguages(Collection<String> languageTags, boolean assignedLanguagesOnly);
}
