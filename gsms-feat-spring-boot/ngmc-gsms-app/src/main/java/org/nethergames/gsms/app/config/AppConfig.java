package org.nethergames.gsms.app.config;

import de.jkeylockmanager.manager.KeyLockManager;
import de.jkeylockmanager.manager.KeyLockManagers;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@ComponentScan({
		"org.nethergames.gsms.app.interceptor",
		"org.nethergames.gsms.app.reflections",
		"org.nethergames.gsms.app.kubernetes",
		"org.nethergames.gsms.domain.repository",
		"org.nethergames.gsms.infra.evaluator",
		"org.nethergames.gsms.infra.repository",
		"org.nethergames.gsms.infra.service",
		"org.nethergames.gsms.infra.listener",
		"org.nethergames.gsms.domain.mapper"
})
public class AppConfig {

	@Bean
	public KeyLockManager getLockManager() {
		return KeyLockManagers.newLock(10, TimeUnit.SECONDS);
	}
}
