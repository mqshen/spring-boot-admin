package de.codecentric.boot.admin.server.domain.values;

import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonFormat;
import de.codecentric.boot.admin.server.domain.Operation;
import de.codecentric.boot.admin.server.domain.OperationType;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

@Data
@NoArgsConstructor
public class OperationInfo {

	OperationType operationType;

	@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
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
