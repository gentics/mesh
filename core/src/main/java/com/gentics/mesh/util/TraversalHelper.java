package com.gentics.mesh.util;

import static com.gentics.mesh.api.common.SortOrder.DESCENDING;
import static com.gentics.mesh.api.common.SortOrder.UNSORTED;

import java.util.List;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.api.common.SortOrder;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.generic.MeshEdgeImpl;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.graphdb.Trx;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.EdgeTraversal;
import com.syncleus.ferma.traversals.VertexTraversal;

/**
 * This class contains a collection of traversal methods that can be used for pagination and other traversals.
 * 
 * @author johannes2
 *
 */
public final class TraversalHelper {

	public static <T> Page<? extends T> getPagedResult(VertexTraversal<?, ?, ?> traversal, VertexTraversal<?, ?, ?> countTraversal, String sortBy,
			SortOrder order, int page, int pageSize, int perPage, Class<T> classOfT) throws InvalidArgumentException {

		if (page < 1) {
			throw new InvalidArgumentException("The page must always be positive");
		}
		if (pageSize < 1) {
			throw new InvalidArgumentException("The pageSize must always be positive");
		}

		// Internally we start with page 0
		page = page - 1;

		int low = page * pageSize;
		int upper = low + pageSize - 1;

		int count = (int) countTraversal.count();

		// Only add the filter to the pipeline when the needed parameters were correctly specified.
		if (order != UNSORTED && sortBy != null) {
			traversal = traversal.order((VertexFrame f1, VertexFrame f2) -> {
				if (order == DESCENDING) {
					VertexFrame tmp = f1;
					f1 = f2;
					f2 = tmp;
				}
				return f2.getProperty(sortBy).equals(f1.getProperty(sortBy)) ? 1 : 0;
			});
		}

		List<? extends T> list = traversal.range(low, upper).toListExplicit(classOfT);

		int totalPages = (int) Math.ceil(count / (double) pageSize);
		// Cap totalpages to 1
		if (totalPages == 0) {
			totalPages = 1;
		}

		// Internally the page size was reduced. We need to increment it now that we are finished.
		return new Page<T>(list, count, ++page, totalPages, list.size(), perPage);

	}

	public static <T> Page<? extends T> getPagedResult(VertexTraversal<?, ?, ?> traversal, VertexTraversal<?, ?, ?> countTraversal,
			PagingInfo pagingInfo, Class<T> classOfT) throws InvalidArgumentException {
		return getPagedResult(traversal, countTraversal, pagingInfo.getSortBy(), pagingInfo.getOrder(), pagingInfo.getPage(), pagingInfo.getPerPage(),
				pagingInfo.getPerPage(), classOfT);
	}

	/**
	 * Simple debug method for a vertex traversal. All vertices will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(VertexTraversal<?, ?, ?> traversal) {
		for (MeshVertexImpl v : traversal.toListExplicit(MeshVertexImpl.class)) {
			System.out.println(v.getProperty("name") + " type: " + v.getFermaType() + " json: " + v.toJson());

		}
	}

	/**
	 * Simple debug method for a edge traversal. All edges will be printed out. Don't use this code for production.
	 * 
	 * @param traversal
	 */
	public static void debug(EdgeTraversal<?, ?, ?> traversal) {
		for (MeshEdgeImpl e : traversal.toListExplicit(MeshEdgeImpl.class)) {
			System.out.println(e.getLabel() + " type: " + e.getFermaType() + " json: " + e.toJson());
		}
	}

	public static void printDebugVertices() {
		for (VertexFrame frame : Trx.getFramedLocalGraph().v()) {
			System.out.println(frame.getId() + " " + frame.getProperty("ferma_type") + " " + frame.getProperty("name"));
		}

	}

}
