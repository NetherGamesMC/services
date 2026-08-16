package org.nethergames.gsms.domain.model.values;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nethergames.common.domain.model.BaseValueObject;

public class NetworkProtocol extends BaseValueObject {

	@Min(value = 1, message = "Network port must be between 1 and 65535")
	@Max(value = 65535, message = "Network port must be between 1 and 65535")
	@NotNull(message = "Network port cannot be null")
	private Integer port;

	@NotBlank(message = "Proxy type cannot be blank")
	private String proxyType;

	public NetworkProtocol(Integer port, String proxyType) {
		this.port = port;
		this.proxyType = proxyType;
	}

	public Integer getPort() {
		return port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public String getProxyType() {
		return proxyType;
	}

	public void setProxyType(String proxyType) {
		this.proxyType = proxyType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;

		if (!(o instanceof NetworkProtocol that)) return false;

		return new EqualsBuilder().append(getPort(), that.getPort()).append(getProxyType(), that.getProxyType()).isEquals();
	}

	@Override
	public int hashCode() {
		return new HashCodeBuilder(17, 37).append(getPort()).append(getProxyType()).toHashCode();
	}
}
