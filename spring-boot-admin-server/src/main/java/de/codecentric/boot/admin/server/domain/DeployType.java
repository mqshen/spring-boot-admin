package de.codecentric.boot.admin.server.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum DeployType {
	JAR,
	WAR,
	TaskJar;

	@JsonValue
	public int toValue() {
		return ordinal();
	}
}
