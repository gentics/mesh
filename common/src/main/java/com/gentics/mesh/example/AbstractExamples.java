package com.gentics.mesh.example;

import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Abstract class which contains commonly used method to handle example REST Model POJO's.
 */
public abstract class AbstractExamples {

	/**
	 * Return an ISO-8601 formatted timestamp string of the current date/time.
	 * 
	 * @return
	 */
	public String createTimestamp() {
		return ZonedDateTime.now().format(DateTimeFormatter.ISO_INSTANT);
	}

	/**
	 * Set the paging meta information in the given response.
	 * 
	 * @param response
	 * @param currentPage
	 * @param pageCount
	 * @param perPage
	 * @param totalCount
	 */
	public void setPaging(ListResponse<?> response, long currentPage, long pageCount, long perPage, long totalCount) {
		PagingMetaInfo info = response.getMetainfo();
		info.setCurrentPage(currentPage);
		info.setPageCount(pageCount);
		info.setPerPage(perPage);
		info.setTotalCount(totalCount);
	}

	/**
	 * Create a dummy schema reference with the given name, random uuid and version 1.
	 * 
	 * @param name
	 * @return
	 */
	public SchemaReference getSchemaReference(String name) {
		SchemaReference schemaReference = new SchemaReference();
		schemaReference.setName(name);
		schemaReference.setUuid(randomUUID());
		schemaReference.setVersion("1.0");
		return schemaReference;
	}

	/**
	 * Create a user reference for user Joe Doe.
	 * 
	 * @return
	 */
	public UserReference createUserReference() {
		UserReference reference = new UserReference();
		reference.setUuid(randomUUID());
		reference.setFirstName("Joe");
		reference.setLastName("Doe");
		return reference;
	}

	public MicroschemaReference getMicroschemaReference(String name, String version) {
		return new MicroschemaReference().setName(name).setUuid(randomUUID()).setVersion(version);
	}

	/**
	 * Create a node reference.
	 * 
	 * @return
	 */
	public NodeReference createNodeReference() {
		NodeReference reference = new NodeReference();
		reference.setUuid(randomUUID());
		return reference;
	}

}
