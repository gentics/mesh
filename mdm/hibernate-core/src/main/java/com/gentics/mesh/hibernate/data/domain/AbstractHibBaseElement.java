package com.gentics.mesh.hibernate.data.domain;

import jakarta.persistence.MappedSuperclass;

import com.gentics.mesh.core.data.HibBaseElement;

/**
 * An ancestor of Mesh entity and Hibernate database element, common for all Enterprise Mesh entity implementation.
 * 
 * @author plyhun
 *
 */
@MappedSuperclass
public abstract class AbstractHibBaseElement extends AbstractHibDatabaseElement implements HibBaseElement {

}
