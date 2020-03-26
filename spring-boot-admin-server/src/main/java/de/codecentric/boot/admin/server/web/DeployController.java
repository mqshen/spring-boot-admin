package de.codecentric.boot.admin.server.web;

import de.codecentric.boot.admin.server.config.DeployProperties;
import de.codecentric.boot.admin.server.config.MicroService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import reactor.core.publisher.Mono;

import java.util.List;

@AdminController
@ResponseBody
public class DeployController {
	private final DeployProperties deployProperties;

	public DeployController(DeployProperties deployProperties) {
		this.deployProperties = deployProperties;
	}

	@GetMapping(path = "/deploy", produces = MediaType.APPLICATION_JSON_VALUE)
	public Mono<List<MicroService>> applications() {
		List<MicroService> services = deployProperties.getServices();
		return Mono.just(services);
	}


}
