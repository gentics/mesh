package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.UserReference;

public abstract class AbstractExamples {

	public String getTimestamp() {
		return ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
	}

	public void setPaging(ListResponse<?> response, long currentPage, long pageCount, long perPage, long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);
	}

	public SchemaReference getSchemaReference(String name) {
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName(name);
		schemaReference.setUuid(randomUUID());
		schemaReference.setVersion(1);
		return schemaReference;
	}

	public UserReference getUserReference() {
		UserReference reference = new UserReference();
		reference.setUuid(randomUUID());
		reference.setFirstName("Joe");
		reference.setLastName("Doe");
		return reference;
	}

	public MicroschemaReference getMicroschemaReference(String name, int version) {
		return new MicroschemaReference().setName(name).setUuid(randomUUID()).setVersion(version);
	}

}
