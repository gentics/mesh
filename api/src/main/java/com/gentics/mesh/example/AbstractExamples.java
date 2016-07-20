package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.Date;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.UserReference;

public abstract class AbstractExamples {

	public long getTimestamp() {
		return new Date().getTime();
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
		reference.setName("jdoe42");
		return reference;
	}

	public MicroschemaReference getMicroschemaReference(String name, int version) {
		return new MicroschemaReference().setName(name).setUuid(randomUUID()).setVersion(version);
	}

}
