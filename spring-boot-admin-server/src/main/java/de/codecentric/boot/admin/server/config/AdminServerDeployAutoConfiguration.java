/*
 * Copyright 2014-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.codecentric.boot.admin.server.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import de.codecentric.boot.admin.server.services.ApplicationRegistry;
import de.codecentric.boot.admin.server.services.DeployService;
import de.codecentric.boot.admin.server.web.DeployController;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "spring.boot.admin.deploy", name = "enabled", havingValue = "true",
		matchIfMissing = true)
@EnableConfigurationProperties({ JenkinsProperties.class })
@EnableJpaRepositories("de.codecentric.boot.admin.server.repositories")
@EntityScan("de.codecentric.boot.admin.server.domain")
@ComponentScan("de.codecentric.boot.admin.server.services")
@EnableAspectJAutoProxy
public class AdminServerDeployAutoConfiguration {

	private final JenkinsProperties jenkinsProperties;

	private final DeployService deployService;

	public AdminServerDeployAutoConfiguration(JenkinsProperties jenkinsProperties, DeployService deployService) {
		this.jenkinsProperties = jenkinsProperties;
		this.deployService = deployService;
	}

	// @Bean
	// @ConditionalOnMissingBean
	// public DeployController deployService(ApplicationRegistry applicationRegistry) {
	// return new DeployController(applicationRegistry, deployService);
	// }

	@Bean
	@ConditionalOnMissingBean
	public DeployController deployController(ApplicationRegistry applicationRegistry) {
		return new DeployController(applicationRegistry, deployService);
	}

}
