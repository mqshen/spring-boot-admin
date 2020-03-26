package de.codecentric.boot.admin.server.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@lombok.Data
@ConfigurationProperties(prefix = "spring.boot.admin.deploy.jenkins")
public class JenkinsProperties {

	private String host;

	private String user;

	private String password;

}
