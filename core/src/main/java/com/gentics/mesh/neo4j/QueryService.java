package com.gentics.mesh.neo4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.neo4j.conversion.Result;
import org.springframework.data.neo4j.support.Neo4jTemplate;
import org.springframework.stereotype.Component;

import com.gentics.mesh.paging.MeshPageRequest;
import com.gentics.mesh.paging.PagingInfo;

@Component
public class QueryService {

	@Autowired
	private Neo4jTemplate neo4jTemplate;

	public <T> Page<T> query(String query, String countQuery, Map<String, Object> parameters, PagingInfo pagingInfo, Class<?> classOfT) {
		Result<Map<String, Object>> result = neo4jTemplate.query(query, parameters);
		List<T> nodes = new ArrayList<>();

		for (Map<String, Object> r : result.slice(pagingInfo.getPage() - 1, pagingInfo.getPerPage())) {
			T tag = (T) neo4jTemplate.getDefaultConverter().convert(r.get("n"), classOfT);
			nodes.add(tag);
		}
		Map<String, Object> countResult = neo4jTemplate.query(countQuery, parameters).singleOrNull();
		long total = (Long) countResult.get("count");
		return new PageImpl<T>(nodes, new MeshPageRequest(pagingInfo), total);
	}

}
