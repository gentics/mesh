package com.gentics.mesh.hibernate.data.domain;

import java.time.Instant;
import java.util.Optional;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.user.HibAPITokenData;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.dagger.annotations.ElementTypeKey;
import com.gentics.mesh.util.ETag;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;

/**
 * Entity implementation of {@link HibAPITokenData}
 */
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "HibEntityCache")
@Entity(name = "apitoken")
@ElementTypeKey(ElementType.APITOKEN)
public class HibAPITokenDataImpl extends AbstractHibDatabaseElement implements HibAPITokenData {
	private String name;

	@ManyToOne(targetEntity = HibUserImpl.class)
	private HibUser user;

	private String tokenId;

	private Instant issued;

	private Instant lastUsed;

	private Instant expires;

	@Override
	public String getETag(InternalActionContext ac) {
		// since only the last used timestamp and the validity can change, we only use tokenId, lastUsed and isValid() to generate the etag
		return ETag.hash("%s%s%b".formatted(tokenId, Optional.ofNullable(lastUsed).map(Instant::toString).orElse(""), isValid()));
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public HibUser getUser() {
		return user;
	}

	@Override
	public HibAPITokenData setUser(HibUser user) {
		this.user = user;
		return this;
	}

	@Override
	public String getTokenId() {
		return tokenId;
	}

	@Override
	public HibAPITokenData setTokenId(String tokenId) {
		this.tokenId = tokenId;
		return this;
	}

	@Override
	public Long getIssuedTimestamp() {
		return Optional.ofNullable(issued).map(Instant::toEpochMilli).orElse(null);
	}

	@Override
	public HibAPITokenData setIssuedTimestamp() {
		issued = Instant.now();
		return this;
	}

	@Override
	public Long getLastUsedTimestamp() {
		return Optional.ofNullable(lastUsed).map(Instant::toEpochMilli).orElse(null);
	}

	@Override
	public HibAPITokenData setLastUsedTimestamp() {
		lastUsed = Instant.now();
		return this;
	}

	@Override
	public Long getExpiresTimestamp() {
		return Optional.ofNullable(expires).map(Instant::toEpochMilli).orElse(null);
	}

	@Override
	public HibAPITokenData setExpiresTimestamp(Long expires) {
		this.expires  = Optional.ofNullable(expires).map(Instant::ofEpochMilli).orElse(null);
		return this;
	}

	@Override
	public boolean isValid() {
		if (expires == null) {
			return true;
		} else {
			return expires.isAfter(Instant.now());
		}
	}
}
