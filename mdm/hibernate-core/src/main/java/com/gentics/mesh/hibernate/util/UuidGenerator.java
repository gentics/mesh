package com.gentics.mesh.hibernate.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.util.UUIDUtil;

/**
 * Creates time based UUIDs.
 *
 * Taken from https://www.baeldung.com/java-uuid Section 4.1
 */
public class UuidGenerator {
	private final Random random;

	public UuidGenerator(Random random) {
		this.random = random;
	}

	private long get64LeastSignificantBitsForVersion1() {
		long random63BitLong = random.nextLong() & 0x3FFFFFFFFFFFFFFFL;
		long variant3BitFlag = 0x8000000000000000L;
		return random63BitLong + variant3BitFlag;
	}

	private long get64MostSignificantBitsForVersion1() {
		LocalDateTime start = LocalDateTime.of(1582, 10, 15, 0, 0, 0);
		Duration duration = Duration.between(start, LocalDateTime.now());
		long seconds = duration.getSeconds();
		long nanos = duration.getNano();
		long timeForUuidIn100Nanos = seconds * 10000000 + nanos * 100;
		long least12SignificatBitOfTime = (timeForUuidIn100Nanos & 0x000000000000FFFFL) >> 4;
		long version = 1 << 12;
		return
			(timeForUuidIn100Nanos & 0xFFFFFFFFFFFF0000L) + version + least12SignificatBitOfTime;
	}

	/**
	 * Create a Type 1 UUID.
	 * The most significant 64 bits are based on the current time.
	 * The least significant 64 bits are random.
	 * @return
	 */
	public UUID generateType1UUID() {
		long most64SigBits = get64MostSignificantBitsForVersion1();
		long least64SigBits = get64LeastSignificantBitsForVersion1();

		return new UUID(most64SigBits, least64SigBits);
	}

	/**
	 * Convert the given UUID string into the java entity, or generate new one, if null.
	 * 
	 * @param uuid
	 * @param uuidGenerator
	 * @return
	 */
	public UUID toJavaUuidOrGenerate(String uuid) {
		if (StringUtils.isNotBlank(uuid)) {
			return UUIDUtil.toJavaUuid(uuid);
		} else {
			return generateType1UUID();
		}
	}
}
