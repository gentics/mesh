package com.gentics.mesh.core.field.date;

import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.util.DateUtils.fromISO8601;
import static com.gentics.mesh.util.DateUtils.toISO8601;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibDateField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.util.DateUtils;

@MeshTestSetting(testSize = FULL, startServer = true)
public class DateFieldEndpointTest extends AbstractFieldEndpointTest {
	private static final String FIELD_NAME = "dateField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
			dateFieldSchema.setName(FIELD_NAME);
			dateFieldSchema.setLabel("Some label");
			prepareTypedSchema(schemaContainer("folder"), List.of(dateFieldSchema), Optional.empty());
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		DateFieldImpl field = response.getFields().getDateField(FIELD_NAME);
		assertNull(field);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		disableAutoPurge();

		HibNode node = folder("2015");
		for (int i = 0; i < 20; i++) {
			Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis() + (i * 10000)));
			HibNodeFieldContainer container;
			Long oldValue;

			try (Tx tx = tx()) {
				container = tx.contentDao().getFieldContainer(node, "en");
				oldValue = getDateValue(container, FIELD_NAME);
			}

			NodeResponse response = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
			DateFieldImpl field = response.getFields().getDateField(FIELD_NAME);
			assertEquals("The timestamp did not match up.", toISO8601(nowEpoch), field.getDate());

			try (Tx tx = tx()) {
				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getDateValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		HibNode node = folder("2015");
		NodeResponse secondResponse;

		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		String oldVersion = firstResponse.getVersion();

		secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME)).as("Field Value").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getDate(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getDate(FIELD_NAME)).isNotNull();
			Long oldValue = latest.getPreviousVersion().getDate(FIELD_NAME).getDate();
			assertThat(oldValue).isEqualTo(nowEpoch);
		}

		try (Tx tx = tx()) {
			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		String oldVersion = firstResponse.getVersion();

		// Date fields can't be set to empty.
		NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldImpl());
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME)).as("Field Value").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);
	}

	@Test
	public void testDateFormatInvalidValue() {
		String invalidDate = "2017-08-21T10:46:26+0200";
		updateNodeFailure(FIELD_NAME, new DateFieldImpl().setDate(invalidDate), BAD_REQUEST, "error_date_format_invalid", invalidDate);
	}

	@Test
	public void testDateFormatValidInstant() {
		Instant now = Instant.now();
		String nowInstant = DateTimeFormatter.ISO_INSTANT.format(now);
		Long nowEpoch = fromISO8601(toISO8601(now.toEpochMilli()));
		NodeResponse response = createNode(FIELD_NAME, new DateFieldImpl().setDate(nowInstant));
		DateField field = response.getFields().getDateField(FIELD_NAME);
		assertEquals(toISO8601(nowEpoch), field.getDate());
	}

	@Test
	public void testDateFormatValidLocal() {
		LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);
		String nowInstant = DateTimeFormatter.ISO_DATE_TIME.format(now);
		Long nowEpoch = fromISO8601(toISO8601(now.toInstant(DateUtils.ZONE_OFFSET).toEpochMilli()));
		NodeResponse response = createNode(FIELD_NAME, new DateFieldImpl().setDate(nowInstant));
		DateField field = response.getFields().getDateField(FIELD_NAME);
		assertEquals(toISO8601(nowEpoch), field.getDate());
	}

	/**
	 * Get the date value
	 *
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return date value (may be null)
	 */
	protected Long getDateValue(HibNodeFieldContainer container, String fieldName) {
		HibDateField field = container.getDate(fieldName);
		return field != null ? field.getDate() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		NodeResponse response = createNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
		DateField field = response.getFields().getDateField(FIELD_NAME);
		assertEquals(toISO8601(nowEpoch), field.getDate());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Long nowEpoch;
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();

			nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			container.createDate(FIELD_NAME).setDate(nowEpoch);
			tx.success();
		}

		NodeResponse response = readNode(folder("2015"));
		DateField deserializedDateField = response.getFields().getDateField(FIELD_NAME);
		assertNotNull(deserializedDateField);
		assertEquals(toISO8601(nowEpoch), deserializedDateField.getDate());
	}

	@Override
	public NodeResponse createNodeWithField() {
		Long nowEpoch = fromISO8601(toISO8601(System.currentTimeMillis()));
		return createNode(FIELD_NAME, new DateFieldImpl().setDate(toISO8601(nowEpoch)));
	}
}
