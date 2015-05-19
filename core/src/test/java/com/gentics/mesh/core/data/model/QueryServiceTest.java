package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;

import com.gentics.mesh.neo4j.QueryService;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;

public class QueryServiceTest extends AbstractDBTest {

	@Autowired
	private QueryService queryService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testSimpleQuery() {

		PagingInfo pagingInfo = new PagingInfo(1, 5);

		String query = "MATCH (n:Tag) return n";
		String countQuery = "MATCH (t:Tag) return count(t) as count";
		Page<Tag> tagPage = queryService.query(query, countQuery, Collections.emptyMap(), pagingInfo, Tag.class);
		for (Tag t : tagPage) {
			System.out.println(t.getUuid());
		}

		// Exclude the root tag
		assertEquals(data().getTags().size() - 1, tagPage.getTotalElements());

	}
}
