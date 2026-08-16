package org.nethergames.gsms.app.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gsms", ignoreUnknownFields = false)
public class ApplicationProperties {

	public Kubernetes kubernetes = new Kubernetes();

	public String serverName;

	public static class Kubernetes {
		public String gameNamespace;

		public String infraNamespace;

		public String getGameNamespace() {
			return gameNamespace;
		}

		public void setGameNamespace(String gameNamespace) {
			this.gameNamespace = gameNamespace;
		}

		public String getInfraNamespace() {
			return infraNamespace;
		}

		public void setInfraNamespace(String infraNamespace) {
			this.infraNamespace = infraNamespace;
		}
	}

	public Kubernetes getKubernetes() {
		return kubernetes;
	}

	public void setKubernetes(Kubernetes kubernetes) {
		this.kubernetes = kubernetes;
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}
}
