package com.gentics.mesh.assertj.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.gentics.mesh.util.StreamUtil;
import org.assertj.core.api.AbstractObjectAssert;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;

public class NodeGraphFieldContainerAssert extends AbstractObjectAssert<NodeGraphFieldContainerAssert, HibNodeFieldContainer> {

	public NodeGraphFieldContainerAssert(HibNodeFieldContainer actual) {
		super(actual, NodeGraphFieldContainerAssert.class);
	}

	/**
	 * Assert that the field uses the given schema.
	 * 
	 * @param schemaVersion
	 *            schema container
	 * @return fluent API
	 */
	public NodeGraphFieldContainerAssert isOf(HibSchemaVersion schemaVersion) {
		assertThat(actual.getSchemaContainerVersion().getVersion()).as("Schema version").isEqualTo(schemaVersion.getVersion());
		assertThat(actual.getSchemaContainerVersion().getUuid()).as("Schema version Uuid").isEqualTo(schemaVersion.getUuid());
		return this;
	}

	/**
	 * Assert that the field container is of the version
	 *
	 * @param version
	 * @return
	 */
	public NodeGraphFieldContainerAssert hasVersion(String version) {
		assertThat(actual.getVersion()).as(descriptionText() + " version").isNotNull().hasToString(version);
		return this;
	}

	/**
	 * Assert that the field container has no previous container
	 * 
	 * @return fluent API
	 */
	public NodeGraphFieldContainerAssert isFirst() {
		assertThat(actual.getPreviousVersion()).as(descriptionText() + " previous container").isNull();
		return this;
	}

	/**
	 * Assert that the field container has no next container
	 * 
	 * @return fluent API
	 */
	public NodeGraphFieldContainerAssert isLast() {
		ContentDao contentDao = Tx.get().contentDao();
		assertThat(contentDao.getNextVersions(actual)).as(descriptionText() + " next container").isEmpty();
		return this;
	}

	/**
	 * Assert that the field container has the given next container
	 * 
	 * @param container
	 * @return fluent API
	 */
	public NodeGraphFieldContainerAssert hasNext(HibNodeFieldContainer container) {
		ContentDao contentDao = Tx.get().contentDao();
		List<HibNodeFieldContainer> next = StreamUtil.toStream(contentDao.getNextVersions(actual)).collect(Collectors.toList());
		assertThat(next).as(descriptionText() + " next container").isNotNull().contains(container);
		return this;
	}

	/**
	 * Assert that the field container has the given previous container
	 * 
	 * @param container
	 * @return fluent API
	 */
	public NodeGraphFieldContainerAssert hasPrevious(HibNodeFieldContainer container) {
		assertThat(actual.getPreviousVersion()).as(descriptionText() + " previous container").isNotNull()
				.isEqualTo(container);
		return this;
	}
}
