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

package de.codecentric.boot.admin.server.web;

import java.net.URISyntaxException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.domain.entities.Application;
import de.codecentric.boot.admin.server.domain.entities.DeployApplication;
import de.codecentric.boot.admin.server.domain.entities.DeployInstance;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.DeployRequest;
import de.codecentric.boot.admin.server.domain.values.DeployServerRequest;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import de.codecentric.boot.admin.server.services.ApplicationRegistry;
import de.codecentric.boot.admin.server.services.DeployService;

@AdminController
@ResponseBody
public class DeployController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeployController.class);

	private final ApplicationRegistry registry;

	private final DeployService deployService;

	// private final Map<String, String> buildMap;

	public DeployController(ApplicationRegistry registry, DeployService deployService) {
		this.registry = registry;
		this.deployService = deployService;
	}

	@GetMapping(path = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<List<DeployApplication>> applications() {
		return registry.getApplications().collectList().map((applications) -> {
			List<DeployApplication> deployApplications = StreamSupport
					.stream(deployService.getService().spliterator(), true).map((service) -> {
						List<DeployInstance> deployInstances;
						Optional<Application> applicationOptional = applications.stream()
								.filter((application) -> service.getName().toUpperCase().equals(application.getName()))
								.findFirst();
						if (applicationOptional.isPresent()) {
							Application application = applicationOptional.get();
							deployInstances = service.getServers().stream().map((server) -> {
								Optional<Instance> instanceOptional = application.getInstances().stream()
										.filter((instance) -> instance.getRegistration().getServiceUrl()
												.contains(server.getHost()))
										.findFirst();
								JenkinsBuild jenkinsBuild = deployService.getBuildInfo(service.getJobName(), server);
								if (instanceOptional.isPresent()) {
									return new DeployInstance(server.getId(), server.getHost(),
											instanceOptional.get().getStatusInfo(), jenkinsBuild);
								}
								else {
									return new DeployInstance(server.getId(), server.getHost(),
											StatusInfo.valueOf("UNKNOWN"), jenkinsBuild);
								}
							}).collect(Collectors.toList());
						}
						else {
							deployInstances = service.getServers().stream().map((server) -> {
								JenkinsBuild jenkinsBuild = deployService.getBuildInfo(service.getJobName(), server);
								return new DeployInstance(server.getId(), server.getHost(),
										StatusInfo.valueOf("UNKNOWN"), jenkinsBuild);
							}).collect(Collectors.toList());
						}
						return new DeployApplication(service.getId(), service.getName(), deployInstances);
					}).collect(Collectors.toList());
			return deployApplications;
		});
	}

	@PostMapping(path = "/deploy/shutdown/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doShutdown(@PathVariable("deployId") Long deployId) {
		return Mono.just("stop need implement");
	}

	@PostMapping(path = "/deploy/build/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doBuild(@PathVariable("deployId") Long deployId) throws URISyntaxException {
		LOGGER.debug("start jenkins build");
		String queue = deployService.startBuild(deployId);
		return Mono.just(queue);
	}

	@GetMapping(path = "/deploy/detail/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public JenkinsBuild queryDetail(@PathVariable("deployId") Long deployId) {
		return deployService.getBuildInfoById(deployId);
	}

	@GetMapping(path = "/deploy/stop/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean doStop(@PathVariable("deployId") Long deployId) {
		return deployService.stopBuild(deployId);
	}

	@GetMapping(path = "/deploy/log/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String queryBuildLog(@PathVariable("deployId") Long deployId) {
		return deployService.getBuildLog(deployId);
	}

	@PostMapping(path = "/deploy/add", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addDeploy(@RequestBody DeployRequest deployRequest) {
		return deployService.addDeploy(deployRequest);
	}

	@PostMapping(path = "/deploy/server", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addDeployServer(@RequestBody DeployServerRequest deployServerRequest) {
		return deployService.addDeployServer(deployServerRequest);
	}

}
