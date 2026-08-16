package org.nethergames.gsms.domain.model.values;

import jakarta.validation.constraints.NotBlank;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nethergames.common.domain.model.BaseValueObject;

public class NetworkEndpoint extends BaseValueObject {

	@NotBlank(message = "Network IP address cannot be blank")
	private String ip;

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (o == null || getClass() != o.getClass()) return false;

		NetworkEndpoint that = (NetworkEndpoint) o;

		return new EqualsBuilder().append(getIp(), that.getIp()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(getIp()).toHashCode();
	}
}
