package com.gentics.mesh.search.index;

import java.util.concurrent.ThreadLocalRandom;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.search.BucketableElement;

import io.reactivex.Flowable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class BucketManagerImpl implements BucketManager {

	private static final Logger log = LoggerFactory.getLogger(BucketManagerImpl.class);

	private final MeshOptions options;

	private final Database database;

	@Inject
	public BucketManagerImpl(MeshOptions options, Database database) {
		this.options = options;
		this.database = database;
	}

	@Override
	public int getBucketPartitionCount(long elementCount) {
		long batchSize = batchSize();
		if (batchSize <= 0) {
			return 1;
		}
		double sizeP = (double) elementCount / (double) batchSize;
		sizeP = Math.ceil(sizeP);
		int size = (int) sizeP;
		// Cap to 1 partition
		if (size == 0) {
			size = 1;
		}
		return size;
	}

	private int batchSize() {
		return options.getSearchOptions().getSyncBatchSize();
	}

	@Override
	public void store(BucketableElement element) {
		if (element.getBucketId() != null) {
			//TODO remove me
			throw new RuntimeException("BucketId already generated");
		}
		int bucketId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);
		log.info("Stored bucketId {" + bucketId + "} for type {" + element.getClass().getSimpleName() + "}");
		element.setBucketId(bucketId);
	}

	@Override
	public Flowable<BucketPartition> getBucketPartitions(Class<? extends BucketableElement> clazz) {
		long count = database.tx(() -> database.count(clazz));
		int partitionCount = getBucketPartitionCount(count);
		int partitionSize = Integer.MAX_VALUE / partitionCount;
		log.info("Calculated {" + partitionCount + "} partitions are needed for {" + count + "} elements and batch size of {" + batchSize() + "}");
		return Flowable.range(0, partitionCount).map(partition -> {
			long start = partitionSize * partition.intValue();
			long end = start - 1 + partitionSize;
			// Cap to end to prevent issues with loss of precision during division
			if (partitionCount - 1 == partition) {
				end = Integer.MAX_VALUE;
			}
			return new BucketPartition(start, end);
		});
	}

}
