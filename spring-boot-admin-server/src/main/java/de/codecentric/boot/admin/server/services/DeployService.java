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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.PostConstruct;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import de.codecentric.boot.admin.server.config.JenkinsProperties;
import de.codecentric.boot.admin.server.domain.DeployInstance;
import de.codecentric.boot.admin.server.domain.DeployServer;
import de.codecentric.boot.admin.server.domain.Environment;
import de.codecentric.boot.admin.server.domain.MicroService;
import de.codecentric.boot.admin.server.domain.Operation;
import de.codecentric.boot.admin.server.domain.OperationType;
import de.codecentric.boot.admin.server.domain.entities.Application;
import de.codecentric.boot.admin.server.domain.entities.DeployApplication;
import de.codecentric.boot.admin.server.domain.entities.DeployInstanceInfo;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.DeployInstanceRequest;
import de.codecentric.boot.admin.server.domain.values.DeployServerRequest;
import de.codecentric.boot.admin.server.domain.values.InstanceId;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import de.codecentric.boot.admin.server.domain.values.OperationInfo;
import de.codecentric.boot.admin.server.domain.values.ServerInfo;
import de.codecentric.boot.admin.server.domain.values.ServiceRequest;
import de.codecentric.boot.admin.server.domain.values.StatusInfo;
import de.codecentric.boot.admin.server.repositories.DeployInstanceRepository;
import de.codecentric.boot.admin.server.repositories.DeployServerRepository;
import de.codecentric.boot.admin.server.repositories.EnvironmentRepository;
import de.codecentric.boot.admin.server.repositories.MicroServiceRepository;
import de.codecentric.boot.admin.server.repositories.OperationRepository;
import de.codecentric.boot.admin.server.web.InstanceWebProxy;
import de.codecentric.boot.admin.server.web.client.InstanceWebClient;

@Service
public class DeployService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeployService.class);

	private static final String ProtocolSchema = "http://";

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
	private OperationRepository operationRepository;

	@Value("${eureka.client.serviceUrl.defaultZone}")
	private String eurekaAddress;

	@Autowired
	InstanceWebClient.Builder instanceWebClientBuilder;

	// @Autowired
	// JenkinsService jenkinsService;

	@Autowired
	private JenkinsProperties jenkinsProperties;

	InstanceWebProxy instanceWebProxy;

	@PostConstruct
	public void init() {
		InstanceWebClient instanceWebClient = instanceWebClientBuilder.build();
		if (instanceWebClient != null) {
			instanceWebProxy = new InstanceWebProxy(instanceWebClient);
		}
	}

	public Iterable<MicroService> getService() {
		return microServiceRepository.findAll();
	}

	public Flux<DeployInstanceInfo> getAllApplicationStream() {
		Map<Long, DeployServer> deployServers = StreamSupport
				.stream(deployServerRepository.findAll().spliterator(), true)
				.collect(Collectors.toMap(DeployServer::getId, Function.identity()));

		return Flux.from(registry.getInstanceEventPublisher())
				.flatMap((event) -> this.instanceRegistry.getInstance(event.getInstance())).map((instance) -> {
					DeployInstanceInfo info = StreamSupport
							.stream(microServiceRepository.findAll().spliterator(), true).filter(
									(service) -> service
											.getName().toUpperCase().equals(instance.getRegistration().getName()))
							.findFirst().map(
									(service) -> deployInstanceRepository.findByServiceId(service.getId()).stream()
											.filter((server) -> instance.getRegistration().getServiceUrl()
													.contains(deployServers.get(server.getServerId()).getName()))
											.findFirst()
											.map((deployInstance) -> generateDeployInstanceInfo(
													deployServers.get(deployInstance.getServerId()), service,
													deployInstance, Optional.of(instance)))
											.orElse(DeployInstanceInfo.empty()))
							.orElse(DeployInstanceInfo.empty());
					return info;
				});
	}

	// public Flux<DeployInstanceInfo> getJenkinsBuild() {
	// return Flux.from(jenkinsService.getJenkinsPublisher()).flatMap((deployId) -> {
	// DeployInstance deployInstance = deployInstanceRepository.findById(deployId).get();
	// MicroService microService =
	// microServiceRepository.findById(deployInstance.getServiceId()).get();
	// DeployServer deployServer =
	// deployServerRepository.findById(deployInstance.getServerId()).get();
	// return Mono.just(generateDeployInstanceInfo(deployServer, microService,
	// deployInstance, Optional.empty()));
	// });
	// }

	public Mono<List<DeployApplication>> getAllApplication() {
		return getAllApplication(registry.getApplications().collectList());
	}

	public Mono<List<DeployApplication>> getAllApplication(Mono<List<Application>> applicationsMono) {
		Map<Long, DeployServer> deployServers = StreamSupport
				.stream(deployServerRepository.findAll().spliterator(), true)
				.collect(Collectors.toMap(DeployServer::getId, Function.identity()));
		return applicationsMono.map((applications) -> {
			List<DeployApplication> deployApplications = StreamSupport
					.stream(microServiceRepository.findAll().spliterator(), true).map((service) -> {
						List<DeployInstanceInfo> deployInstancesInfo;
						List<DeployInstance> deployInstances = deployInstanceRepository
								.findByServiceId(service.getId());
						Optional<Application> applicationOptional = applications.stream()
								.filter((application) -> service.getName().toUpperCase().equals(application.getName()))
								.findFirst();
						if (applicationOptional.isPresent()) {
							Application application = applicationOptional.get();
							deployInstancesInfo = deployInstances.stream().map((server) -> {
								Optional<Instance> instanceOptional = application.getInstances().stream()
										.filter((instance) -> instance.getRegistration().getServiceUrl()
												.contains(deployServers.get(server.getServerId()).getName()))
										.findFirst();

								return generateDeployInstanceInfo(deployServers.get(server.getServerId()), service,
										server, instanceOptional);
							}).collect(Collectors.toList());
						}
						else {
							deployInstancesInfo = deployInstances.stream()
									.map((server) -> generateDeployInstanceInfo(deployServers.get(server.getServerId()),
											service, server, Optional.empty()))
									.collect(Collectors.toList());
						}
						return new DeployApplication(service.getId(), service.getName(), service.getJobName(),
								service.getProjectName(), service.getDeployType(), service.isAutoStart(),
								service.getBranch(), service.getRollbackBranch(), service.getProfile(),
								service.getPort(), service.getPath(), service.getEnv(), service.getParameter(),
								deployInstancesInfo);
					}).collect(Collectors.toList());
			return deployApplications;
		});
	}

	public Optional<JenkinsBuild> getBuildInfoById(Long deployId) {
		return deployInstanceRepository.findById(deployId).map((deployInstance) -> {
			MicroService microService = microServiceRepository.findById(deployInstance.getServiceId()).get();
			return getBuildInfo(microService.getJobName(), deployInstance);
		});
	}

	public String startBuild(Long deployId) throws URISyntaxException {
		return startBuild(deployId, false);
	}

	public String startBuild(Long instanceId, boolean rollback) throws URISyntaxException {
		// jenkinsService.startListener(instanceId);

		Operation operation;
		if (rollback) {
			operation = new Operation(instanceId, OperationType.ROLLBACK);
		}
		else {
			operation = new Operation(instanceId, OperationType.DEPLOY);
		}
		operationRepository.save(operation);

		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
				jenkinsProperties.getUser(), jenkinsProperties.getPassword());
		Optional<DeployInstance> deployServiceOptional = deployInstanceRepository.findById(instanceId);
		if (deployServiceOptional.isPresent()) {
			DeployInstance instance = deployServiceOptional.get();
			try {
				MicroService microService = microServiceRepository.findById(instance.getServiceId()).get();
				JobWithDetails jobWithDetails = jenkinsServer.getJob(microService.getJobName());

				Map<String, String> param = new HashMap<>(); // service.getMetadata();
				param.put("projectName", microService.getProjectName());
				DeployServer deployServer = deployServerRepository.findById(instance.getServerId()).get();
				param.put("server", deployServer.getName());

				if (rollback) {
					if (instance.getRollbackBranch() != null) {
						param.put("branch", instance.getRollbackBranch());
					}
					else {
						param.put("branch", microService.getRollbackBranch());
					}
				}
				else {
					if (instance.getBranch() != null) {
						param.put("branch", instance.getBranch());
					}
					else {
						param.put("branch", microService.getBranch());
					}
				}

				if (instance.getProfile() != null) {
					param.put("profile", instance.getProfile());
				}
				else {
					param.put("profile", microService.getProfile());
				}

				param.put("port", String.valueOf(microService.getPort()));
				param.put("deployPath", microService.getPath());

				if (eurekaAddress != null) {
					int index = eurekaAddress.indexOf("/eureka");
					String eurekaUrl = eurekaAddress;
					if (index > 0) {
						eurekaUrl = eurekaAddress.substring(0, index);
					}
					param.put("eurekaAddress", eurekaUrl);
				}

				QueueReference reference = jobWithDetails.build(param, true);
				String itemUrl = reference.getQueueItemUrlPart();
				instance.setQueueId(itemUrl);
				deployInstanceRepository.save(instance);
				return itemUrl;
			}
			catch (IOException ex) {
				LOGGER.error("error push job to jenkins", ex);
			}
		}
		return "";
	}

	public boolean stopBuild(Long deployId) {
		Optional<DeployInstance> deployServiceOptional = deployInstanceRepository.findById(deployId);
		if (deployServiceOptional.isPresent()) {
			DeployInstance deployInstance = deployServiceOptional.get();
			MicroService microService = microServiceRepository.findById(deployInstance.getServiceId()).get();
			Pair<Optional<Build>, Boolean> pair = getBuild(microService.getJobName(), deployInstance);
			if (pair != null) {
				Optional<Build> build = pair.getFirst();
				try {
					if (build.isPresent()) {
						BuildWithDetails buildWithDetails = build.get().details();
						updateDeployBuildId(deployInstance, buildWithDetails.getId());
						build.get().Stop(true);
						return true;
					}
				}
				catch (IOException ex) {
					LOGGER.error("error push job to jenkins", ex);
				}
			}
		}
		return false;
	}

	private void updateDeployBuildId(DeployInstance deployInstance, String buildId) {
		deployInstance.setLastBuildId(buildId);
		deployInstance.setQueueId("");
		deployInstanceRepository.save(deployInstance);
	}

	public JenkinsBuild getBuildInfo(String jobName, DeployInstance deployInstance) {
		try {
			Pair<Optional<Build>, Boolean> pair = getBuild(jobName, deployInstance);
			if (pair != null) {
				Optional<Build> build = pair.getFirst();
				JenkinsBuild jenkinsBuild;
				if (build.isPresent()) {
					BuildWithDetails buildWithDetails = build.get().details();
					updateDeployBuildId(deployInstance, buildWithDetails.getId());

					jenkinsBuild = new JenkinsBuild(pair.getSecond(), buildWithDetails.isBuilding(),
							buildWithDetails.getDuration(), buildWithDetails.getEstimatedDuration(),
							buildWithDetails.getTimestamp());
					// if (!jenkinsBuild.isBuilding() && !jenkinsBuild.isQueued()) {
					// jenkinsService.stopListener(deployInstance.getId());
					// }
				}
				else {
					jenkinsBuild = new JenkinsBuild(pair.getSecond(), false, 0, 0, 0);
				}
				return jenkinsBuild;
			}
		}
		catch (Exception ex) {
			LOGGER.error("query jenkins failed ", ex);
		}
		return new JenkinsBuild();
	}

	public Pair<Optional<Build>, Boolean> getBuild(String jobName, DeployInstance deployInstance) {
		try {
			JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
					jenkinsProperties.getUser(), jenkinsProperties.getPassword());
			JobWithDetails jobWithDetails = jenkinsServer.getJob(jobName);
			Build build = null;
			QueueItem queueItem = null;
			String reference = deployInstance.getQueueId();
			if (!StringUtils.isEmpty(reference)) {
				QueueReference queueReference = new QueueReference(reference);
				queueItem = jenkinsServer.getQueueItem(queueReference);

				if (!queueItem.isCancelled() && jobWithDetails.isInQueue()) {
					return Pair.of(Optional.empty(), queueItem != null);
				}
				build = jenkinsServer.getBuild(queueItem);
			}
			else if (!StringUtils.isEmpty(deployInstance.getLastBuildId())) {
				build = jobWithDetails.getBuildByNumber(Integer.parseInt(deployInstance.getLastBuildId()));
			}
			if (build == null) {
				return Pair.of(Optional.empty(), queueItem != null);
			}
			else {
				return Pair.of(Optional.of(build), queueItem != null);
			}
		}
		catch (Exception ex) {
			LOGGER.error("query jenkins failed ", ex);
		}
		return null;
	}

	public String getBuildLog(Long deployId) {
		Optional<DeployInstance> deployInstanceOptional = deployInstanceRepository.findById(deployId);
		if (deployInstanceOptional.isPresent()) {
			try {
				DeployInstance deployInstance = deployInstanceOptional.get();
				MicroService microService = microServiceRepository.findById(deployInstance.getServiceId()).get();
				Pair<Optional<Build>, Boolean> pair = getBuild(microService.getJobName(), deployInstance);
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

	public Mono<List<ServerInfo>> getAllServer() {
		return registry.getApplications().collectList().map((applications) -> {
			List<ServerInfo> deployServers = StreamSupport.stream(deployServerRepository.findAll().spliterator(), true)
					.map((server) -> {
						List<DeployInstanceInfo> deployInstances;
						List<DeployInstance> instances = deployInstanceRepository.findByServerId(server.getId());
						deployInstances = instances.stream().map((instance) -> {
							MicroService microService = microServiceRepository.findById(instance.getServiceId()).get();
							Optional<Application> applicationOptional = applications.stream().filter(
									(application) -> microService.getName().toUpperCase().equals(application.getName()))
									.findFirst();
							if (applicationOptional.isPresent()) {
								Application application = applicationOptional.get();
								Optional<Instance> instanceOptional = application
										.getInstances().stream().filter((eurekaInstance) -> eurekaInstance
												.getRegistration().getServiceUrl().contains(server.getName()))
										.findFirst();
								return generateDeployInstanceInfo(server, microService, instance, instanceOptional);
							}
							else {
								return generateDeployInstanceInfo(server, microService, instance, Optional.empty());
							}

						}).collect(Collectors.toList());
						Optional<Environment> environment = environmentRepository.findById(server.getEnvironmentId());
						ServerInfo serverInfo = ServerInfo.fromEntity(server, environment.get());
						serverInfo.setInstances(deployInstances);
						return serverInfo;
					}).collect(Collectors.toList());
			return deployServers;
		});
	}

	private DeployInstanceInfo generateDeployInstanceInfo(DeployServer server, MicroService microService,
			DeployInstance deployInstance, Optional<Instance> instanceOptional) {
		Optional<Operation> operationOptional = operationRepository
				.findFirstByInstanceIdOrderByOpTimeDesc(deployInstance.getId());
		OperationInfo operationInfo = OperationInfo.fromEntity(operationOptional);
		StatusInfo statusInfo;
		String sbaId = null;
		if (instanceOptional.isPresent()) {
			statusInfo = instanceOptional.get().getStatusInfo();
			sbaId = instanceOptional.get().getId().getValue();
		}
		else {
			statusInfo = StatusInfo.valueOf("UNKNOWN");
		}
		JenkinsBuild jenkinsBuild = getBuildInfo(microService.getJobName(), deployInstance);

		return new DeployInstanceInfo(deployInstance.getId(), sbaId, microService.getName(), server.getName(),
				ProtocolSchema + server.getName() + ":" + microService.getPort(), deployInstance.getServiceGroup(),
				deployInstance.getBranch(), deployInstance.getRollbackBranch(), deployInstance.getProfile(), statusInfo,
				jenkinsBuild, operationInfo);
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

	public Mono<Boolean> shutdown(String instanceId, Long deployInstanceId) {
		Operation operation = new Operation(deployInstanceId, OperationType.DEPLOY);
		operationRepository.save(operation);

		URI uri = UriComponentsBuilder.fromPath("/shutdown").build(true).toUri();
		Mono<Instance> instanceMono = this.instanceRegistry.getInstance(InstanceId.of(instanceId));
		Mono<ClientResponse> clientResponseMono = instanceWebProxy.forward(instanceMono, uri, HttpMethod.POST,
				new HttpHeaders(), BodyInserters.empty());
		return clientResponseMono.map((response) -> response.statusCode().equals(HttpStatus.OK));
	}

	public List<ServerInfo> listServers() {
		return StreamSupport.stream(deployServerRepository.findAll().spliterator(), true).map(
				(deployServer) -> new ServerInfo(deployServer.getId(), deployServer.getName(), deployServer.getIp()))
				.collect(Collectors.toList());
	}

}
