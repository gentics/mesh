package com.gentics.mesh.plugin.forms.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

public class BinaryElementTest {

	@Test
	public void extractFieldId_plainKey() {
		assertThat(BinaryElement.extractFieldId(URLEncoder.encode("$.properties[\"file1\"].value", StandardCharsets.UTF_8))).hasValue(Pair.of("file1", 0));
	}

	@Test
	public void extractFieldId_uuidKey() {
		assertThat(BinaryElement.extractFieldId(URLEncoder.encode("$.properties[\"CE2FD5CF-AB33-4165-BD5E-46EE358E4A4E\"].value", StandardCharsets.UTF_8)))
			.hasValue(Pair.of("CE2FD5CF-AB33-4165-BD5E-46EE358E4A4E", 0));
	}

	@Test
	public void extractFieldId_valuesIndexKey() {
		assertThat(BinaryElement.extractFieldId(URLEncoder.encode("$.properties[\"CE2FD5CF\"].values[1]", StandardCharsets.UTF_8))).hasValue(Pair.of("CE2FD5CF", 1));
	}

	@Test
	public void extractFieldId_legacyPlainId_returnsNull() {
		assertThat(BinaryElement.extractFieldId("file1")).isEmpty();
	}

	@Test
	public void extractFieldId_null_returnsNull() {
		assertThat(BinaryElement.extractFieldId(null)).isEmpty();
	}
}
