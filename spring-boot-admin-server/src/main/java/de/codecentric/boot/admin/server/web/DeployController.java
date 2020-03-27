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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.*;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.config.DeployProperties;
import de.codecentric.boot.admin.server.config.JenkinsProperties;
import de.codecentric.boot.admin.server.config.MicroServiceProperties;
import de.codecentric.boot.admin.server.domain.entities.Application;
import de.codecentric.boot.admin.server.domain.entities.DeployApplication;
import de.codecentric.boot.admin.server.domain.entities.DeployInstance;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.DeployRequest;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import de.codecentric.boot.admin.server.services.ApplicationRegistry;

@AdminController
@ResponseBody
public class DeployController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeployController.class);

	private final DeployProperties deployProperties;

	private final ApplicationRegistry registry;

	private final Map<String, String> buildMap;

	public DeployController(DeployProperties deployProperties, ApplicationRegistry registry) {
		this.deployProperties = deployProperties;
		this.registry = registry;
		buildMap = new HashMap<>();
	}

	@GetMapping(path = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<List<DeployApplication>> applications() {
		List<MicroServiceProperties> services = deployProperties.getServices();
		return registry.getApplications().collectList().map((applications) -> {
			List<DeployApplication> deployApplications = services.stream().map((service) -> {
				List<DeployInstance> deployInstances;
				Optional<Application> applicationOptional = applications.stream()
						.filter((application) -> service.getName().toUpperCase().equals(application.getName()))
						.findFirst();
				if (applicationOptional.isPresent()) {
					Application application = applicationOptional.get();
					deployInstances = service.getServer().stream().map((server) -> {
						Optional<Instance> instanceOptional = application.getInstances().stream()
								.filter((instance) -> instance.getRegistration().getServiceUrl().contains(server))
								.findFirst();
						JenkinsBuild jenkinsBuild = getBuildInfo(service.getJenkinsJobName(),
							service.getProjectName(), server);
						if (instanceOptional.isPresent()) {
							return new DeployInstance(server, instanceOptional.get().getStatusInfo(), jenkinsBuild);
						}
						else {
							return new DeployInstance(server, StatusInfo.valueOf("UNKNOWN"), jenkinsBuild);
						}
					}).collect(Collectors.toList());
				}
				else {
					deployInstances = service.getServer().stream()
							.map((server) -> {
								JenkinsBuild jenkinsBuild = getBuildInfo(service.getJenkinsJobName(),
									service.getProjectName(), server);
								return new DeployInstance(server, StatusInfo.valueOf("UNKNOWN"), jenkinsBuild);
							}).collect(Collectors.toList());
				}
				return new DeployApplication(service.getName(), deployInstances);
			}).collect(Collectors.toList());
			return deployApplications;
		});
	}

	@PostMapping(path = "/deploy/stop", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doStop(@RequestBody DeployRequest deployRequest) {
		return Mono.just("stop need implement");
	}

	@PostMapping(path = "/deploy/build", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doBuild(@RequestBody DeployRequest deployRequest) throws URISyntaxException {
		LOGGER.debug("start jenkins build");
		JenkinsProperties jenkinsProperties = deployProperties.getJenkins();
		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
				jenkinsProperties.getUser(), jenkinsProperties.getPassword());
		Optional<MicroServiceProperties> serviceOptional = deployProperties.getServices().stream()
				.filter((service) -> deployRequest.getName().equals(service.getName())).findFirst();
		if (serviceOptional.isPresent()) {
			MicroServiceProperties service = serviceOptional.get();
			Optional<String> serverOptional = service.getServer().stream()
					.filter((server) -> deployRequest.getServer().equals(server)).findFirst();
			if (serverOptional.isPresent()) {
				String server = serverOptional.get();
				try {
					JobWithDetails jobWithDetails = jenkinsServer.getJob(service.getJenkinsJobName());

					Map<String, String> param = service.getMetadata();
					if (param == null) {
						param = new HashMap<>();
					}
					param.put("projectName", service.getProjectName());
					param.put("server", server);
					QueueReference reference = jobWithDetails.build(param, true);
					String itemUrl = reference.getQueueItemUrlPart();
					buildMap.put(service.getProjectName() + "-" + server, itemUrl);
					return Mono.just(itemUrl);
				}
				catch (IOException ex) {
					LOGGER.error("error push job to jenkins", ex);
				}
			}
		}
		return Mono.just("");
	}

	@PostMapping(path = "/deploy/detail", produces = MediaType.APPLICATION_JSON_VALUE)
	public JenkinsBuild queryDetail(@RequestBody DeployRequest deployRequest) {
		Optional<MicroServiceProperties> serviceOptional = deployProperties.getServices().stream()
			.filter((service) -> deployRequest.getName().equals(service.getName())).findFirst();
		if (serviceOptional.isPresent()) {
			MicroServiceProperties service = serviceOptional.get();
			return getBuildInfo(service.getJenkinsJobName(), deployRequest.getName(), deployRequest.getServer());
		}
		return new JenkinsBuild();
	}

	protected JenkinsBuild getBuildInfo(String jobName, String projectName, String server) {
		try {
			String reference = buildMap.get(projectName + "-" + server);
			if (reference != null) {
				JenkinsProperties jenkinsProperties = deployProperties.getJenkins();
				JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
					jenkinsProperties.getUser(), jenkinsProperties.getPassword());
				QueueReference queueReference = new QueueReference(reference);
				JobWithDetails jobWithDetails = jenkinsServer.getJob(jobName);
				QueueItem queueItem = jenkinsServer.getQueueItem(queueReference);

				if (!queueItem.isCancelled() && jobWithDetails.isInQueue()) {
					return new JenkinsBuild(true, false);
				}
				Build build = jenkinsServer.getBuild(queueItem);
				if (build != null) {
					BuildWithDetails buildWithDetails = build.details();
					queueItem = jobWithDetails.getQueueItem();
					JenkinsBuild jenkinsBuild = new JenkinsBuild(queueItem != null, buildWithDetails.isBuilding(),
						buildWithDetails.getDuration(), buildWithDetails.getEstimatedDuration(),
						buildWithDetails.getTimestamp()
					);
					return jenkinsBuild;
				}
			}
		}
		catch (Exception ex) {
			LOGGER.error("query jenkins failed ", ex);
		}
		return new JenkinsBuild();
	}
}
