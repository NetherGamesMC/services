package org.nethergames.gsms.app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfiguration implements AsyncConfigurer, WebMvcConfigurer {

	@Bean(name = "virtualTaskExecutor")
	public ScheduledExecutorService getVirtualAsyncExecutor() {
		ThreadFactory factory = Thread.ofPlatform().name("gsms-vt").factory();
		return Executors.newScheduledThreadPool(2, factory);
	}
}
