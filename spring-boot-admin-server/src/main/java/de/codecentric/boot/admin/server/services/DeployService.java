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

package de.codecentric.boot.admin.server.services;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import com.offbytwo.jenkins.model.Build;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.domain.DeployInstance;
import de.codecentric.boot.admin.server.domain.DeployServer;
import de.codecentric.boot.admin.server.domain.MicroService;
import de.codecentric.boot.admin.server.domain.Operation;
import de.codecentric.boot.admin.server.domain.OperationType;
import de.codecentric.boot.admin.server.domain.entities.DeployApplication;
import de.codecentric.boot.admin.server.domain.entities.DeployInstanceInfo;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.BuildRequest;
import de.codecentric.boot.admin.server.domain.values.DeployInstanceRequest;
import de.codecentric.boot.admin.server.domain.values.DeployServerRequest;
import de.codecentric.boot.admin.server.domain.values.EnvironmentInfo;
import de.codecentric.boot.admin.server.domain.values.GroupInfo;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import de.codecentric.boot.admin.server.domain.values.OperationInfo;
import de.codecentric.boot.admin.server.domain.values.ServerInfo;
import de.codecentric.boot.admin.server.domain.values.ServiceRequest;
import de.codecentric.boot.admin.server.domain.values.Shutdown;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import de.codecentric.boot.admin.server.repositories.DeployInstanceRepository;
import de.codecentric.boot.admin.server.repositories.DeployServerRepository;
import de.codecentric.boot.admin.server.repositories.EnvironmentRepository;
import de.codecentric.boot.admin.server.repositories.GroupRepository;
import de.codecentric.boot.admin.server.repositories.MicroServiceRepository;
import de.codecentric.boot.admin.server.repositories.OperationRepository;
import de.codecentric.boot.admin.server.web.InstanceWebProxy;
import de.codecentric.boot.admin.server.web.client.InstanceWebClient;

@Service
public class DeployService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeployService.class);

	private static final String ProtocolSchema = "http://";

	private final AtomicBoolean processing;

	@Autowired
	private ApplicationRegistry registry;

	@Autowired
	private InstanceRegistry instanceRegistry;

	@Autowired
	private DeployInstanceRepository deployInstanceRepository;

	@Autowired
	private DeployServerRepository deployServerRepository;

	@Autowired
	private MicroServiceRepository microServiceRepository;

	@Autowired
	private EnvironmentRepository environmentRepository;

	@Autowired
	private GroupRepository groupRepository;

	@Autowired
	private OperationRepository operationRepository;

	@Autowired
	InstanceWebClient.Builder instanceWebClientBuilder;

	@Autowired
	JenkinsService jenkinsService;

	InstanceWebProxy instanceWebProxy;

	private Map<Long, DeployServer> deployServers;

	private Map<Long, DeployInstance> deployInstances;

	private Map<Long, MicroService> microServices;

	private final Map<Long, Instance> instanceMap;

	@Autowired
	private TaskExecutor taskExecutor;

	public DeployService() {
		this.instanceMap = new HashMap<>();
		processing = new AtomicBoolean(false);
	}

	protected Optional<DeployInstance> getDeployInstance(Instance instance) {
		Optional<DeployInstance> optionalDeployInstance = deployInstances.values().stream().filter((deployInstance) -> {
			MicroService microService = microServices.get(deployInstance.getServiceId());
			if (microService.getName().toUpperCase().equals(instance.getRegistration().getName())) {
				DeployServer deployServer = deployServers.get(deployInstance.getServerId());
				return instance.getRegistration().getServiceUrl().contains(deployServer.getName())
						|| instance.getRegistration().getServiceUrl().contains(deployServer.getIp());
			}
			else {
				return false;
			}
		}).findFirst();
		optionalDeployInstance.ifPresent((deployServer) -> instanceMap.put(deployServer.getId(), instance));
		return optionalDeployInstance;
	}

	protected void initDeployServer() {
		deployServers = StreamSupport.stream(deployServerRepository.findAll().spliterator(), true)
				.collect(Collectors.toMap(DeployServer::getId, Function.identity()));
	}

	protected void initMicroService() {
		microServices = StreamSupport.stream(microServiceRepository.findAll().spliterator(), true)
				.collect(Collectors.toMap(MicroService::getId, Function.identity()));
	}

	protected void initDeployInstance() {
		deployInstances = StreamSupport.stream(deployInstanceRepository.findAll().spliterator(), true)
				.collect(Collectors.toMap(DeployInstance::getId, Function.identity()));
	}

	@PostConstruct
	public void init() {
		InstanceWebClient instanceWebClient = instanceWebClientBuilder.build();
		if (instanceWebClient != null) {
			instanceWebProxy = new InstanceWebProxy(instanceWebClient);
		}
		initDeployServer();
		initMicroService();
		initDeployInstance();

		taskExecutor.execute(() -> {
			Flux.from(registry.getInstanceEventPublisher())
					.flatMap((event) -> this.instanceRegistry.getInstance(event.getInstance())).map((instance) -> {
						getDeployInstance(instance);
						return true;
					});
		});

		LOGGER.info("deploy service init complete");
	}

	@Scheduled(initialDelay = 10000, fixedRate = 5 * 60 * 1000)
	public void initInstanceMap() {
		if (!processing.getAndSet(true)) {
			// registry.getApplications().collectList().map((applications) ->{
			instanceRegistry.getInstances().map((instance) -> {
				getDeployInstance(instance);
				return true;
			}).doOnComplete(() -> {
				System.out.println("complete");
				processing.set(false);
			}).collectList().block();
			System.out.println("finish");
		}
	}

	public Flux<Optional<DeployInstance>> doRefresh() {
		initDeployServer();
		initMicroService();
		initDeployInstance();
		return instanceRegistry.getInstances().map((instance) -> getDeployInstance(instance));
	}

	public Iterable<MicroService> getService() {
		return microServiceRepository.findAll();
	}

	public Flux<DeployInstanceInfo> getAllApplicationStream() {
		return Flux.from(registry.getInstanceEventPublisher())
				.flatMap((event) -> this.instanceRegistry.getInstance(event.getInstance()))
				.map((instance) -> getDeployInstance(instance)
						.map((deployInstance) -> generateDeployInstanceInfo(deployInstance))
						.orElse(DeployInstanceInfo.empty()));
	}

	public List<DeployApplication> getAllApplication() {
		return microServices.values().stream().map((service) -> {
			List<Long> deployInstancesInfo = deployInstances.values().stream()
					.filter((deployInstance) -> deployInstance.getServiceId().equals(service.getId()))
					.map((deployInstance) -> deployInstance.getId()).collect(Collectors.toList());

			return new DeployApplication(service.getId(), service.getName(), service.getJobName(),
					service.getProjectName(), service.getDeployType(), service.isAutoStart(), service.getBranch(),
					service.getRollbackBranch(), service.getProfile(), service.getPort(), service.getPath(),
					service.getEnv(), service.getParameter(), deployInstancesInfo);
		}).collect(Collectors.toList());
	}

	public List<ServerInfo> getAllServer() {
		return deployServers.values().stream().map((server) -> {
			List<Long> deployInstancesInfo = deployInstances.values().stream()
					.filter((deployInstance) -> deployInstance.getServerId().equals(server.getId()))
					.map((deployInstance) -> deployInstance.getId()).collect(Collectors.toList());

			ServerInfo serverInfo = ServerInfo.fromEntity(server);
			serverInfo.setChildren(deployInstancesInfo);
			return serverInfo;
		}).collect(Collectors.toList());
	}

	public String startBuild(Long deployId) throws URISyntaxException {
		return startBuild(deployId, false, false);
	}

	public String start(List<Long> deployIdArray) throws URISyntaxException {
		try {
			return startBuild(deployIdArray, true);
		}
		catch (URISyntaxException e) {
			LOGGER.error("failed start instance ", e);
			return "";
		}
	}

	public Pair<String, String> test(Pair<StringBuilder, StringBuilder> sb, Pair<String, String> item) {
		Pair<String, String> of = Pair.of(sb.getFirst() + item.getFirst(), sb.getSecond() + item.getSecond());
		return of;
	}

	public String startBuild(List<Long> instances, boolean onlyStart) throws URISyntaxException {
		List<Operation> operations = instances.stream()
				.map((instanceId) -> new Operation(instanceId, OperationType.DEPLOY)).collect(Collectors.toList());
		operationRepository.saveAll(operations);

		Map<String, String> param = new HashMap<>(); // service.getMetadata();
		DeployInstance deployInstance = deployInstances.get(instances.get(0));
		if (deployInstance != null) {
			MicroService microService = microServices.get(deployInstance.getServiceId());
			param.put("projectName", microService.getProjectName());
			param.put("branch", microService.getBranch());
			if (onlyStart) {
				param.put("onlyStart", "true");
			}
			else  {
				param.put("onlyStart", "false");
			}
			param.put("deployPath", microService.getPath());
			param.put("profile", microService.getProfile());
			param.put("port", String.valueOf(microService.getPort()));

			Pair<String, String> stream = instances.stream().map((instanceId) -> {
				DeployInstance instance = deployInstances.get(instanceId);
				if (instance != null) {
					DeployServer deployServer = deployServers.get(instance.getServerId());
					return Pair.of(deployServer.getIp(), deployServer.getUser());
				}
				return Pair.of("", "");
			}).filter((item) -> item.getFirst().length() > 0).reduce(Pair.of("", ""), (l, r) -> {
				if (l.getFirst().isEmpty()) {
					return r;
				}
				String first = l.getFirst() + "," + r.getFirst();
				String second = l.getSecond() + "," + r.getSecond();
				return Pair.of(first, second);
			});
			String ips = stream.getFirst();
			String users = stream.getSecond();
			param.put("server", ips);
			param.put("userName", users);
			try {
				String itemUrl = jenkinsService.sendBuild(microService.getJobName(), param);
				deployInstance.setQueueId(itemUrl);
				deployInstanceRepository.save(deployInstance);
				return itemUrl;
			}
			catch (IOException ex) {
				LOGGER.error("error push job to jenkins", ex);
			}
		}
		return "";
	}

	public String startBuild(Long instanceId, boolean rollback, boolean onlyStart) throws URISyntaxException {

		Operation operation;
		if (rollback) {
			operation = new Operation(instanceId, OperationType.ROLLBACK);
		}
		else {
			operation = new Operation(instanceId, OperationType.DEPLOY);
		}
		operationRepository.save(operation);

		DeployInstance deployInstance = deployInstances.get(instanceId);
		if (deployInstance != null) {
			try {
				MicroService microService = microServices.get(deployInstance.getServiceId());

				Map<String, String> param = new HashMap<>(); // service.getMetadata();
				param.put("projectName", microService.getProjectName());
				DeployServer deployServer = deployServers.get(deployInstance.getServerId());
				param.put("server", deployServer.getIp());
				param.put("userName", deployServer.getUser());

				if (rollback) {
					if (deployInstance.getRollbackBranch() != null) {
						param.put("branch", deployInstance.getRollbackBranch());
					}
					else {
						param.put("branch", microService.getRollbackBranch());
					}
				}
				else {
					if (deployInstance.getBranch() != null) {
						param.put("branch", deployInstance.getBranch());
					}
					else {
						param.put("branch", microService.getBranch());
					}
				}

				if (onlyStart) {
					param.put("onlyStart", "true");
				}
				else {
					param.put("onlyStart", "false");
				}

				if (deployInstance.getProfile() != null) {
					param.put("profile", deployInstance.getProfile());
				}
				else {
					param.put("profile", microService.getProfile());
				}

				param.put("port", String.valueOf(microService.getPort()));
				param.put("deployPath", microService.getPath());

				String itemUrl = jenkinsService.sendBuild(microService.getJobName(), param);
				deployInstance.setQueueId(itemUrl);
				deployInstanceRepository.save(deployInstance);
				return itemUrl;
			}
			catch (IOException ex) {
				LOGGER.error("error push job to jenkins", ex);
			}
		}
		return "";
	}

	public boolean stopBuild(Long deployId) {
		DeployInstance deployInstance = deployInstances.get(deployId);
		if (deployInstance != null) {
			MicroService microService = microServiceRepository.findById(deployInstance.getServiceId()).get();
			jenkinsService.stopBuild(microService.getJobName(), deployInstance);
		}
		return false;
	}

	public String getBuildLog(Long deployId) {
		Optional<DeployInstance> deployInstanceOptional = deployInstanceRepository.findById(deployId);
		if (deployInstanceOptional.isPresent()) {
			try {
				DeployInstance deployInstance = deployInstanceOptional.get();
				MicroService microService = microServiceRepository.findById(deployInstance.getServiceId()).get();
				Pair<Optional<Build>, Boolean> pair = jenkinsService.getBuild(microService.getJobName(),
						deployInstance);
				if (pair != null) {
					Optional<Build> build = pair.getFirst();
					if (build.isPresent()) {
						return build.get().details().getConsoleOutputText();
					}
				}
			}
			catch (Exception ex) {
				LOGGER.error("query jenkins console log failed ", ex);
			}
		}
		return "";
	}

	public Long addService(ServiceRequest serviceRequest) {
		if (serviceRequest.getId() == null || serviceRequest.getId() == 0) {
			MicroService microService = new MicroService(serviceRequest.getName(), serviceRequest.getJobName(),
					serviceRequest.getProjectName(), serviceRequest.getDeployType(), serviceRequest.isAutoStart(),
					serviceRequest.getBranch(), serviceRequest.getRollbackBranch(), serviceRequest.getProfile(),
					serviceRequest.getPort(), serviceRequest.getPath(), serviceRequest.getEnv(),
					serviceRequest.getParameter());
			return microService.getId();
		}
		else {
			return microServiceRepository.findById(serviceRequest.getId()).map((microService) -> {
				microService.setJobName(serviceRequest.getJobName());
				microService.setProjectName(serviceRequest.getProjectName());
				microService.setDeployType(serviceRequest.getDeployType());
				microService.setAutoStart(serviceRequest.isAutoStart());
				microService.setBranch(serviceRequest.getBranch());
				microService.setRollbackBranch(serviceRequest.getRollbackBranch());
				microService.setProfile(serviceRequest.getProfile());
				microService.setPort(serviceRequest.getPort());
				microService.setPath(serviceRequest.getPath());
				microService.setEnv(serviceRequest.getEnv());
				microService.setParameter(serviceRequest.getParameter());
				microServiceRepository.save(microService);
				return microService.getId();
			}).orElse(0L);
		}
	}

	public Long addDeployInstance(DeployInstanceRequest deployInstanceRequest) {
		if (deployInstanceRequest.getId() != null && deployInstanceRequest.getId() != 0) {
			return deployInstanceRepository.findById(deployInstanceRequest.getId()).map((deployInstance) -> {
				deployInstance.setServiceGroup(deployInstanceRequest.getGroup());
				deployInstance.setBranch(deployInstanceRequest.getBranch());
				deployInstance.setRollbackBranch(deployInstanceRequest.getRollbackBranch());
				deployInstance.setProfile(deployInstanceRequest.getProfile());
				deployInstanceRepository.save(deployInstance);
				return deployInstance.getId();
			}).orElse(0L);
		}
		else {
			return microServiceRepository.findById(deployInstanceRequest.getServiceId()).map((microService) -> {
				DeployInstance deployInstance = new DeployInstance(microService.getId(),
						deployInstanceRequest.getServerId());
				deployInstance.setServiceGroup(deployInstanceRequest.getGroup());
				deployInstance.setBranch(deployInstanceRequest.getBranch());
				deployInstance.setRollbackBranch(deployInstanceRequest.getRollbackBranch());
				deployInstance.setProfile(deployInstanceRequest.getProfile());
				deployInstance = deployInstanceRepository.save(deployInstance);
				return deployInstance.getId();
			}).orElse(0L);
		}
	}

	public Long addDeployServer(DeployServerRequest deployServerRequest) {
		if (deployServerRequest.getId() == null || deployServerRequest.getId() == 0) {
			DeployServer deployServer = new DeployServer(deployServerRequest.getEnvironmentId(),
					deployServerRequest.getName(), deployServerRequest.getIp(), deployServerRequest.getLoginType(),
					deployServerRequest.getUser(), deployServerRequest.getPassword());
			deployServer = deployServerRepository.save(deployServer);
			return deployServer.getId();
		}
		else {
			return deployServerRepository.findById(deployServerRequest.getId()).map((deployServer) -> {
				deployServer.setEnvironmentId(deployServerRequest.getEnvironmentId());
				deployServer.setName(deployServerRequest.getName());
				deployServer.setIp(deployServerRequest.getIp());
				deployServer.setLoginType(deployServerRequest.getLoginType());
				deployServer.setUser(deployServerRequest.getUser());
				deployServer.setPassword(deployServerRequest.getPassword());
				deployServerRepository.save(deployServer);
				return deployServer.getId();
			}).orElse(0L);
		}
	}

	private DeployInstanceInfo generateDeployInstanceInfo(DeployInstance deployInstance) {
		MicroService microService = microServices.get(deployInstance.getServiceId());
		DeployServer deployServer = deployServers.get(deployInstance.getServerId());
		Optional<Operation> operationOptional = operationRepository
				.findFirstByInstanceIdOrderByOpTimeDesc(deployInstance.getId());
		OperationInfo operationInfo = OperationInfo.fromEntity(operationOptional);
		StatusInfo statusInfo;
		String sbaId = null;
		Instance instance = instanceMap.get(deployInstance.getId());
		if (instance != null) {
			statusInfo = instance.getStatusInfo();
			sbaId = instance.getId().getValue();
		}
		else {
			statusInfo = StatusInfo.valueOf("DOWN");
		}
		JenkinsBuild jenkinsBuild = new JenkinsBuild(deployInstance.getStatus());
		// jenkinsService.getBuildInfo(microService.getJobName(), deployInstance);

		return new DeployInstanceInfo(deployInstance.getId(), sbaId, microService.getName(),
				deployInstance.getServerId(), ProtocolSchema + deployServer.getName() + ":" + microService.getPort(),
				deployInstance.getServiceGroup(), deployInstance.getBranch(), deployInstance.getRollbackBranch(),
				deployInstance.getProfile(), statusInfo, jenkinsBuild, operationInfo);
	}

	public Mono<List<Boolean>> shutdown(List<Shutdown> instances) {
		Stream<Mono<Boolean>> mono = instances.stream().map((instance) -> {
			Operation operation = new Operation(instance.getDeployInstanceId(), OperationType.DEPLOY);
			operationRepository.save(operation);

			URI uri = UriComponentsBuilder.fromPath("/shutdown").build(true).toUri();
			Mono<Instance> instanceMono = this.instanceRegistry.getInstance(InstanceId.of(instance.getInstanceId()));
			Mono<ClientResponse> clientResponseMono = instanceWebProxy.forward(instanceMono, uri, HttpMethod.POST,
				new HttpHeaders(), BodyInserters.empty());
			return clientResponseMono.map((response) -> response.statusCode().equals(HttpStatus.OK));
		});
		Mono<List<Boolean>> result = Flux.fromStream(mono).flatMap(x -> x).collectList();
		return result;
	}

	public List<ServerInfo> listServers() {
		return deployServers.values().stream().map((deployServer) -> ServerInfo.fromEntity(deployServer))
				.collect(Collectors.toList());
	}

	public List<DeployInstanceInfo> listInstance() {
		return deployInstances.values().stream().map((deployInstance) -> generateDeployInstanceInfo(deployInstance))
				.collect(Collectors.toList());
	}

	public List<EnvironmentInfo> listEnvironments() {
		return StreamSupport.stream(environmentRepository.findAll().spliterator(), true)
				.map((environment) -> EnvironmentInfo.fromEntity(environment)).collect(Collectors.toList());
	}

	public List<GroupInfo> listGroup() {
		return StreamSupport.stream(groupRepository.findAll().spliterator(), true)
				.map((group) -> GroupInfo.fromEntity(group)).collect(Collectors.toList());
	}
}
