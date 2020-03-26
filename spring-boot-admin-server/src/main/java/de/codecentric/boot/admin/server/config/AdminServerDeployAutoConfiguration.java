package de.codecentric.boot.admin.server.config;

import de.codecentric.boot.admin.server.web.DeployController;
import de.codecentric.boot.admin.server.web.JenkinsController;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.boot.admin.deploy", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties({ DeployProperties.class})
public class AdminServerDeployAutoConfiguration {

	private final DeployProperties deployProperties;

	public AdminServerDeployAutoConfiguration(DeployProperties deployProperties) {
		this.deployProperties = deployProperties;
	}

	@Bean
	@ConditionalOnMissingBean
	public DeployController deployController() {
		return new DeployController(deployProperties);
	}

}
