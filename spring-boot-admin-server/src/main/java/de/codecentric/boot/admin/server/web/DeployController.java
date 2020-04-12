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
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.domain.DeployInstance;
import de.codecentric.boot.admin.server.domain.entities.DeployApplication;
import de.codecentric.boot.admin.server.domain.entities.DeployInstanceInfo;
import de.codecentric.boot.admin.server.domain.values.BuildRequest;
import de.codecentric.boot.admin.server.domain.values.DeployInstanceRequest;
import de.codecentric.boot.admin.server.domain.values.DeployServerRequest;
import de.codecentric.boot.admin.server.domain.values.EnvironmentInfo;
import de.codecentric.boot.admin.server.domain.values.GroupInfo;
import de.codecentric.boot.admin.server.domain.values.ServerInfo;
import de.codecentric.boot.admin.server.domain.values.ServiceRequest;
import de.codecentric.boot.admin.server.domain.values.ShutdownRequest;
import de.codecentric.boot.admin.server.services.DeployService;

@AdminController
@ResponseBody
public class DeployController {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeployController.class);

	private static final ServerSentEvent<?> PING = ServerSentEvent.builder().comment("ping").build();

	private static final Flux<ServerSentEvent<?>> PING_FLUX = Flux.interval(Duration.ZERO, Duration.ofSeconds(10L))
			.map((tick) -> PING);

	private final DeployService deployService;

	public DeployController(DeployService deployService) {
		this.deployService = deployService;
	}

	@GetMapping(path = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<DeployApplication> applications() {
		return deployService.getAllApplication();
	}

	@GetMapping(path = "/deploy", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<ServerSentEvent<DeployInstanceInfo>> applicationsStream() {
		return deployService.getAllApplicationStream()
				.map((application) -> ServerSentEvent.builder(application).build()).mergeWith(ping());
	}

	@GetMapping(path = "/deploy/list/instances", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<DeployInstanceInfo> listInstance() {
		return deployService.listInstance();
	}

	@GetMapping(path = "/deploy/list/servers", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ServerInfo> listServers() {
		return deployService.listServers();
	}

	@PostMapping(path = "/deploy/service", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addService(@RequestBody ServiceRequest serviceRequest) {
		return deployService.addService(serviceRequest);
	}

	@PostMapping(path = "/deploy/shutdown", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<List<Boolean>> doShutdown(@RequestBody ShutdownRequest shutdownRequest) {
		LOGGER.debug("shutdown an instance");
		return deployService.shutdown(shutdownRequest.getInstances());
	}

	@PostMapping(path = "/deploy/buildAll", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doBuildAll(@RequestBody BuildRequest request) throws URISyntaxException {
		LOGGER.debug("start jenkins build");
		String queue = deployService.startBuild(request.getInstances(), false);
		return Mono.just(queue);
	}

	@PostMapping(path = "/deploy/build/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doBuild(@PathVariable("deployId") Long deployId) throws URISyntaxException {
		LOGGER.debug("start jenkins build");
		String queue = deployService.startBuild(deployId);
		return Mono.just(queue);
	}

	@PostMapping(path = "/deploy/start", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doStart(@RequestBody BuildRequest request) throws URISyntaxException {
		LOGGER.debug("start jenkins build");
		String queue = deployService.start(request.getInstances());
		return Mono.just(queue);
	}

	@PostMapping(path = "/deploy/rollback/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<String> doRollback(@PathVariable("deployId") Long deployId) throws URISyntaxException {
		LOGGER.debug("start jenkins rollback");
		String queue = deployService.startBuild(deployId, true, false);
		return Mono.just(queue);
	}

	// @GetMapping(path = "/deploy/detail/{deployId}", produces =
	// MediaType.APPLICATION_JSON_VALUE)
	// public JenkinsBuild queryDetail(@PathVariable("deployId") Long deployId) {
	// return deployService.getBuildInfoById(deployId).orElse(new JenkinsBuild());
	// }

	@PostMapping(path = "/deploy/instance", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addDeployInstance(@RequestBody DeployInstanceRequest deployInstanceRequest) {
		return deployService.addDeployInstance(deployInstanceRequest);
	}

	@GetMapping(path = "/deploy/stop/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean doStop(@PathVariable("deployId") Long deployId) {
		return deployService.stopBuild(deployId);
	}

	@GetMapping(path = "/deploy/log/{deployId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public String queryBuildLog(@PathVariable("deployId") Long deployId) {
		return deployService.getBuildLog(deployId);
	}

	@GetMapping(path = "/deploy/server", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ServerInfo> getDeployServer() {
		return deployService.getAllServer();
	}

	@PostMapping(path = "/deploy/server", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addDeployServer(@RequestBody DeployServerRequest deployServerRequest) {
		return deployService.addDeployServer(deployServerRequest);
	}

	@GetMapping(path = "/deploy/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
	public Flux<Optional<DeployInstance>> dorefresh() {
		return deployService.doRefresh();
	}

	@GetMapping(path = "/deploy/environments", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<EnvironmentInfo> queryEnvironments() {
		return deployService.listEnvironments();
	}

	@GetMapping(path = "/deploy/groups", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<GroupInfo> queryGroups() {
		return deployService.listGroup();
	}

	private static <T> Flux<ServerSentEvent<T>> ping() {
		return (Flux<ServerSentEvent<T>>) (Flux) PING_FLUX;
	}

}
