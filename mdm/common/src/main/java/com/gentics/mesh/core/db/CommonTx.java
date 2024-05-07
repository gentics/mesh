package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.dao.PersistingBinaryDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingContentDao;
import com.gentics.mesh.core.data.dao.PersistingGroupDao;
import com.gentics.mesh.core.data.dao.PersistingImageVariantDao;
import com.gentics.mesh.core.data.dao.PersistingJobDao;
import com.gentics.mesh.core.data.dao.PersistingLanguageDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingNodeDao;
import com.gentics.mesh.core.data.dao.PersistingProjectDao;
import com.gentics.mesh.core.data.dao.PersistingRoleDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.PersistingTagDao;
import com.gentics.mesh.core.data.dao.PersistingTagFamilyDao;
import com.gentics.mesh.core.data.dao.PersistingUserDao;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A developer API of {@link Tx}.
 * 
 * @author plyhun
 *
 */
public interface CommonTx extends Tx, TxEntityPersistenceManager {

	/**
	 * Return the current active common transaction.
	 * 
	 * @return Currently active transaction
	 */
	static CommonTx get() {
		return (CommonTx) Tx.get();
	}

	@Override
	default int txId() {
		return hashCode();
	}

	@Override
	default EventQueueBatch createBatch() {
		return data().getOrCreateEventQueueBatch();
	}

	@Override
	PersistingImageVariantDao imageVariantDao();

	@Override
	PersistingBinaryDao binaryDao();

	@Override
	PersistingNodeDao nodeDao();

	@Override
	PersistingUserDao userDao();

	@Override
	PersistingGroupDao groupDao();

	@Override
	PersistingRoleDao roleDao();

	@Override
	PersistingProjectDao projectDao();

	@Override
	PersistingLanguageDao languageDao();

	@Override
	PersistingJobDao jobDao();

	@Override
	PersistingTagFamilyDao tagFamilyDao();

	@Override
	PersistingTagDao tagDao();

	@Override
	PersistingBranchDao branchDao();

	@Override
	PersistingMicroschemaDao microschemaDao();

	@Override
	PersistingSchemaDao schemaDao();

	@Override
	PersistingContentDao contentDao();

	@Override
	CommonTxData data();
}
