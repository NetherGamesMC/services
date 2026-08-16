package org.nethergames.gsms.app.config;

import jakarta.servlet.ServletContext;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class WebConfigurer implements ServletContextInitializer {

	private final Logger log = LoggerFactory.getLogger(WebConfigurer.class);

	private final Environment env;

	public WebConfigurer(Environment env, ApplicationProperties applicationProperties) {
		this.env = env;
	}

	@Override
	public void onStartup(@NonNull ServletContext servletContext) {
		if (env.getActiveProfiles().length != 0) {
			log.info("Web application configuration, using profiles: {}", (Object[]) env.getActiveProfiles());
		}
	}
}