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

package de.codecentric.boot.admin.server.domain.values;

import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import de.codecentric.boot.admin.server.domain.Operation;
import de.codecentric.boot.admin.server.domain.OperationType;

@Data
@NoArgsConstructor
public class OperationInfo {

	OperationType operationType;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
	Date opTime;

	public OperationInfo(OperationType operationType, Date opTime) {
		this.operationType = operationType;
		this.opTime = opTime;
	}

	public static OperationInfo fromEntity(Operation operation) {
		return new OperationInfo(operation.getOperationType(), operation.getOpTime());
	}

	public static OperationInfo fromEntity(Optional<Operation> operationOptional) {
		if (operationOptional.isPresent()) {
			Operation operation = operationOptional.get();
			return new OperationInfo(operation.getOperationType(), operation.getOpTime());
		}
		else {
			return new OperationInfo(OperationType.None, new Date());
		}
	}

}
