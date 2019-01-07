package com.gentics.mesh.changelog;

import java.util.Iterator;
import java.util.UUID;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Vertex;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;
import com.gentics.madl.tx.Tx;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Basic implementation of a changelog change. All change implementations should extend this class.
 */
public abstract class AbstractChange implements Change {

	protected static final Logger log = LoggerFactory.getLogger(AbstractChange.class);

	private Tx tx;

	private long duration;

	@Override
	public abstract void apply();

	@Override
	public abstract String getUuid();

	@Override
	public boolean isApplied() {
		ChangelogRootWrapper changelogRoot = changelogRoot();
		return changelogRoot.hasChange(getUuid());
	}

	/**
	 * Return the changelog root wrapper. The wrapper wraps the graph element which stored changelog information.
	 * 
	 * @return
	 */
	private ChangelogRootWrapper changelogRoot() {
		Vertex meshRoot = getMeshRootVertex();
		if (meshRoot == null) {
			throw new RuntimeException("Could not find mesh root node. The change can't be applied without the mesh root vertex.");
		}
		Iterator<Vertex> it = meshRoot.vertices(Direction.OUT, ChangelogRootWrapper.HAS_CHANGELOG_ROOT);
		Vertex changelogRoot = null;
		if (it.hasNext()) {
			changelogRoot = it.next();
		}

		// Create the change if it could not be found.
		if (changelogRoot == null) {
			log.debug("The changelog root could not be found. Creating it...");
			changelogRoot = tx.createVertex(ChangelogRootWrapper.class);
			meshRoot.addEdge(ChangelogRootWrapper.HAS_CHANGELOG_ROOT, changelogRoot);
		}
		return new ChangelogRootWrapper(tx, changelogRoot);
	}

	public Vertex getMeshRootVertex() {
		return MeshGraphHelper.getMeshRootVertex(getTx());
	}

	@Override
	public void markAsComplete() {
		changelogRoot().add(this);
	}

	@Override
	public void setTx(Tx tx) {
		this.tx = tx;
	}

	@Override
	public Tx getTx() {
		return tx;
	}

	public static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator();

	/**
	 * Create a random UUID string which does not include dashes.
	 * 
	 * @return
	 */
	public String randomUUID() {
		final UUID uuid = UUID_GENERATOR.generate();
		String randomUuid = (digits(uuid.getMostSignificantBits() >> 32, 8) + digits(uuid.getMostSignificantBits() >> 16, 4) + digits(uuid
			.getMostSignificantBits(), 4) + digits(uuid.getLeastSignificantBits() >> 48, 4) + digits(uuid.getLeastSignificantBits(), 12));
		return randomUuid;
	}

	protected void fail(String msg) {
		throw new RuntimeException(msg);
	}

	/**
	 * Returns val represented by the specified number of hex digits.
	 * 
	 * @param val
	 * @param digits
	 * @return
	 */
	private static String digits(long val, int digits) {
		long hi = 1L << (digits * 4);
		return Long.toHexString(hi | (val & (hi - 1))).substring(1);
	}

	@Override
	public long getDuration() {
		return this.duration;
	}

	@Override
	public void setDuration(long timeMs) {
		this.duration = timeMs;
	}

	@Override
	public boolean validate() {
		return true;
	}

	@Override
	public boolean requiresReindex() {
		return false;
	}

	public void debug(Element element) {
		System.out.println("---");
		for (String key : element.keys()) {
			System.out.println(key + " : " + element.property(key));
		}
		System.out.println("---");
	}

}
