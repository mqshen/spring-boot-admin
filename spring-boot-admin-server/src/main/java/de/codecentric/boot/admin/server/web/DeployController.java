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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.domain.entities.DeployApplication;
import de.codecentric.boot.admin.server.domain.values.DeployInstanceRequest;
import de.codecentric.boot.admin.server.domain.values.DeployServerRequest;
import de.codecentric.boot.admin.server.domain.values.ServerInfo;
import de.codecentric.boot.admin.server.domain.values.ServiceRequest;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import de.codecentric.boot.admin.server.services.DeployService;

@AdminController
@ResponseBody
public class DeployController {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeployController.class);

	private final DeployService deployService;


	public DeployController(DeployService deployService) {
		this.deployService = deployService;
	}

	@GetMapping(path = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<List<DeployApplication>> applications() {
		return deployService.getAllApplication();
	}

	@GetMapping(path = "/deploy/list/servers", produces = MediaType.APPLICATION_JSON_VALUE)
	public List<ServerInfo> listServers() {
		return deployService.listServers();
	}

	@PostMapping(path = "/deploy/service", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addService(@RequestBody ServiceRequest serviceRequest) {
		return deployService.addService(serviceRequest);
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
	public  Mono<List<ServerInfo>> getDeployServer() {
		return deployService.getAllServer();
	}

	@PostMapping(path = "/deploy/server", produces = MediaType.APPLICATION_JSON_VALUE)
	public Long addDeployServer(@RequestBody DeployServerRequest deployServerRequest) {
		return deployService.addDeployServer(deployServerRequest);
	}
}
