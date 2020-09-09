package com.gentics.mesh.example;

import static com.gentics.mesh.example.ExampleUuids.MICROSCHEMA_UUID;
import static com.gentics.mesh.example.ExampleUuids.NODE_DELOREAN_UUID;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_FOLDER_UUID;
import static com.gentics.mesh.example.ExampleUuids.USER_EDITOR_UUID;

import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserReference;

/**
 * Abstract class which contains commonly used method to handle example REST Model POJO's.
 */
public abstract class AbstractExamples {

	public static final String DATE_OLD = "2018-10-12T14:15:06.024Z";

	public static final String DATE_NEW = "2018-11-20T20:12:01.084Z";

	public static final long TIMESTAMP_OLD = 1541744513012L;

	public static final long TIMESTAMP_NEW = 1542746513622L;

	/**
	 * Return an ISO-8601 formatted timestamp.
	 * 
	 * @return
	 */
	public String createNewTimestamp() {
		return DATE_NEW;
	}

	/**
	 * Return an ISO-8601 formatted timestamp.
	 * 
	 * @return
	 */
	public String createOldTimestamp() {
		return DATE_OLD;
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
	public SchemaReferenceImpl getSchemaReference(String name) {
		SchemaReferenceImpl schemaReference = new SchemaReferenceImpl();
		schemaReference.setName(name);
		schemaReference.setUuid(SCHEMA_FOLDER_UUID);
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
		reference.setUuid(USER_EDITOR_UUID);
		reference.setFirstName("Joe");
		reference.setLastName("Doe");
		return reference;
	}

	public MicroschemaReference getMicroschemaReference(String name, String version) {
		return new MicroschemaReferenceImpl().setName(name).setUuid(MICROSCHEMA_UUID).setVersion(version);
	}

	/**
	 * Create a node reference.
	 * 
	 * @return
	 */
	public NodeReference createNodeReference() {
		NodeReference reference = new NodeReference();
		reference.setUuid(NODE_DELOREAN_UUID);
		return reference;
	}

}
