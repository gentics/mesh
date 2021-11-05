package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.core.data.node.field.S3BinaryGraphField;
import com.gentics.mesh.core.data.s3binary.S3HibBinary;
import com.gentics.mesh.core.result.Result;

/**
 * DAO for {@link S3HibBinary} operations.
 */
public interface S3BinaryDaoWrapper extends S3BinaryDao {
    Result<S3BinaryGraphField> findFields(S3HibBinary s3binary);
}
