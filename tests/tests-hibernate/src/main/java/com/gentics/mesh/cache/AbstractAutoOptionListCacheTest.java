package com.gentics.mesh.cache;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.cache.CacheStatusModel;
import com.gentics.mesh.cache.ListableFieldCacheImpl;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.dagger.HibernateMeshComponent;
import com.gentics.mesh.etc.config.HibernateMeshOptions;
import com.gentics.mesh.etc.config.ConfigUtils;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.test.MeshOptionChanger;
import com.gentics.mesh.test.context.AbstractMeshTest;

public abstract class AbstractAutoOptionListCacheTest extends AbstractMeshTest implements MeshOptionChanger {

	private static final String DEFAULT_LIST_FIELD_KEY = "massiveList";

	protected final long numOfItemsPerField;

	public AbstractAutoOptionListCacheTest() {
		this(500);
	}

	public AbstractAutoOptionListCacheTest(long numOfItemsPerField) {
		this.numOfItemsPerField = numOfItemsPerField;
	}

	@Override
	public void change(MeshOptions options) {
		((HibernateMeshOptions) options).getCacheConfig().setListFieldCacheSize(size() + unit());
	}

	@Before
	public void makeEmAll() {
		boolean alreadyCreated = tx(() -> {
			Schema schema = project().getBaseNode().getSchemaContainer();
			FieldSchema field = schema.getLatestVersion().getSchema().getField(DEFAULT_LIST_FIELD_KEY);
			return field != null;
		});
		if (!alreadyCreated) {
			String schemaUuid = tx(() -> project().getBaseNode().getSchemaContainer().getUuid());
			SchemaUpdateRequest update = call(() -> client().findSchemaByUuid(schemaUuid)).toUpdateRequest();
			update.addField(createFieldSchema(DEFAULT_LIST_FIELD_KEY, false));
			waitForJob(() -> client().updateSchema(schemaUuid, update).blockingGet());
		}
		String nodeUuid = tx(() -> project().getBaseNode().getUuid());
		NodeUpdateRequest update = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid)).toRequest();
		StringFieldListImpl field = new StringFieldListImpl();
		for (int i = 0; i < numOfItemsPerField; i++) {
			field.add("Massive List Item " + i + " " + RandomStringUtils.randomAscii(i));
		}
		update.getFields().put(DEFAULT_LIST_FIELD_KEY, field);
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
	}

	@After
	public void nukeEmAll() {
		String nodeUuid = tx(() -> project().getBaseNode().getUuid());
		NodeUpdateRequest update = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid)).toRequest();
		update.getFields().clear();
		call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update));
	}

	protected void checkStats(CacheStatusModel stats) {
		String rawSize = size()  + unit();
		long sizeAvg = parseRawSize(rawSize);
		assertEquals(((HibernateMeshOptions) options()).getCacheConfig().getListFieldCacheSize(), stats.getSetup());
		assertEquals(sizeAvg, stats.getMaxSizeInUnits());
		assertTrue(sizeAvg >= stats.getCurrentSizeInUnits());
		assertTrue(0 < stats.getCurrentSizeInUnits());
	}

	public CacheStatusModel getCacheStats() {
		HibernateMeshComponent mesh = ((HibernateMeshComponent)mesh());
		ListableFieldCacheImpl storage = mesh.listableFieldCacheStorage();
		return storage.getStatus();
	}

	@Test
	public void testReadAll() {
		String uuid = tx(() -> project().getBaseNode().getUuid());
		NodeResponse node = call(() -> client().findNodeByUuid(PROJECT_NAME, uuid));
		StringFieldListImpl field = node.getFields().getStringFieldList(DEFAULT_LIST_FIELD_KEY);
		assertEquals("The node field did not contain the created list items", numOfItemsPerField, field.getItems().size());

		CacheStatusModel stats = getCacheStats();
		checkStats(stats);
	}

	protected abstract String unit();

	protected abstract int size();

	protected ListFieldSchema createFieldSchema(String fieldKey, boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("string");
		schema.setName(fieldKey);
		schema.setRequired(isRequired);
		return schema;
	}

	protected long parseRawSize(String cacheSizeRaw) {
		Matcher percentageMatcher = ConfigUtils.QUOTA_PATTERN_PERCENTAGE.matcher(cacheSizeRaw);
		Matcher sizeMatcher = ConfigUtils.QUOTA_PATTERN_SIZE.matcher(cacheSizeRaw);
		Matcher numberMatcher = ConfigUtils.QUOTA_PATTERN_NUMBER.matcher(cacheSizeRaw);
		if (percentageMatcher.matches()) {
			return Runtime.getRuntime().maxMemory() / 100L * Long.parseLong(percentageMatcher.group("value"));
		} else if (sizeMatcher.matches()) {
			return ConfigUtils.getBytes(sizeMatcher);
		} else if (numberMatcher.matches()) {
			return Long.parseLong(numberMatcher.group("value"));
		} else {
			return 0;
		}
	}
}
