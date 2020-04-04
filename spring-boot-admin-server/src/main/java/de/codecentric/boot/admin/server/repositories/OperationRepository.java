package de.codecentric.boot.admin.server.repositories;

import java.util.Optional;

import de.codecentric.boot.admin.server.domain.Operation;
import org.springframework.data.repository.CrudRepository;

public interface OperationRepository  extends CrudRepository<Operation, Long> {

	Optional<Operation> findFirstByInstanceIdOrderByOpTimeDesc(Long instanceId);

}
