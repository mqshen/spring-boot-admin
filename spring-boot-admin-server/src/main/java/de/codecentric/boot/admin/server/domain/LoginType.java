package de.codecentric.boot.admin.server.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum LoginType {
	PASSWORD,
	CERT;

	@JsonValue
	public int toValue() {
		return ordinal();
	}
}
