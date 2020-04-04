package de.codecentric.boot.admin.server.domain;

import java.util.Date;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
@Table(name = "ifp_operation")
public class Operation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	Long instanceId;

	Date opTime;

	@Enumerated(EnumType.ORDINAL)
	OperationType operationType;
}
