package com.gentics.mesh.util;

import static com.gentics.mesh.util.SortOrder.DESCENDING;
import static com.gentics.mesh.util.SortOrder.UNSORTED;

import java.util.List;

import com.gentics.mesh.util.SortOrder;

import com.gentics.mesh.core.Page;
import com.syncleus.ferma.VertexFrame;
import com.syncleus.ferma.traversals.VertexTraversal;

public class PagingHelper {

	public <T> Page<? extends T> getPagedResult(VertexTraversal<?, ?, ?> traversal, String sortBy, SortOrder order, int page, int pageSize,
			Class<T> classOfT) throws InvalidArgumentException {

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
		//		System.out.println("Page: " + page);
		//		System.out.println("Range: " + low + " to " + (low + pageSize));

		int count = (int) traversal.count();
		//		System.out.println("Found: " + count);
		List<? extends T> list = traversal.order((VertexFrame f1, VertexFrame f2) -> {
			if (order == DESCENDING) {
				VertexFrame tmp = f1;
				f1 = f2;
				f2 = tmp;
			} else if (order == UNSORTED) {
				return 0;
			}

			return f2.getProperty(sortBy).equals(f1.getProperty(sortBy)) ? 1 : 0;

		}).range(low, upper).toList(classOfT);

		int totalPages = count / pageSize;

		// Internally the page size was reduced. We need to increment it now that we are finished.
		return new Page<T>(list, count, ++page, totalPages, list.size());

	}

}
