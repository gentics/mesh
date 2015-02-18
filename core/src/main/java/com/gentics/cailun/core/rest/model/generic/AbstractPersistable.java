package com.gentics.cailun.core.rest.model.generic;

import java.io.Serializable;

import lombok.Data;

import org.springframework.data.neo4j.annotation.Fetch;
import org.springframework.data.neo4j.annotation.GraphId;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract class for all node entities.
 * 
 * @author johannes2
 *
 */
@Data
public abstract class AbstractPersistable implements Serializable {
	private static final long serialVersionUID = -3244769429406745303L;

	/**
	 * The mandatory neo4j graph id for this object.
	 */
	@GraphId
	private Long id;

	/**
	 * The uuid of the object. A transaction event handler is being used in 
	 * order to generate and verify the integrity of uuids.
	 */
	@Fetch
	private String uuid;

	/**
	 * Check whether the object was not yet saved.
	 * 
	 * @return true, when the object was not yet saved. Otherwise false.
	 */
	@JsonIgnore
	public boolean isNew() {
		return null == getId();
	}

	@Override
	public int hashCode() {
		int hashCode = 17;

		hashCode += isNew() ? 0 : getId().hashCode() * 31;

		return hashCode;
	}

	@Override
	public boolean equals(Object obj) {

		if (null == obj) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!getClass().equals(obj.getClass())) {
			return false;
		}

		AbstractPersistable that = (AbstractPersistable) obj;

		return null == this.getId() ? false : this.getId().equals(that.getId());
	}
}
