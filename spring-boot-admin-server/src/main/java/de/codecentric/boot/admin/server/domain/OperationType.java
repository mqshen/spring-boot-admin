package de.codecentric.boot.admin.server.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum OperationType {
	None,
	DEPLOY,
	START,
	SHUTDOWN,
	ROLLBACK;

	@JsonValue
	public int toValue() {
		return ordinal();
	}
}
