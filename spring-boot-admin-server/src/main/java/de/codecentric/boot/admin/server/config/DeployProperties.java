package de.codecentric.boot.admin.server.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "spring.boot.admin.deploy")
public class DeployProperties {

	private JenkinsProperties jenkins;

	private List<MicroService> services;

}
