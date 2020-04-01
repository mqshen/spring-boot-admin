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
import java.util.Map;
import java.util.Optional;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.Build;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.codecentric.boot.admin.server.config.JenkinsProperties;
import de.codecentric.boot.admin.server.domain.DeployServer;
import de.codecentric.boot.admin.server.domain.MicroService;
import de.codecentric.boot.admin.server.domain.values.DeployRequest;
import de.codecentric.boot.admin.server.domain.values.DeployServerRequest;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import de.codecentric.boot.admin.server.repositories.DeployServerRepository;
import de.codecentric.boot.admin.server.repositories.MicroServiceRepository;

@Service
public class DeployService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DeployService.class);

	@Autowired
	private DeployServerRepository deployServerRepository;

	@Autowired
	private MicroServiceRepository microServiceRepository;

	@Autowired
	private JenkinsProperties jenkinsProperties;

	public Iterable<MicroService> getService() {
		return microServiceRepository.findAll();
	}

	public JenkinsBuild getBuildInfoById(Long deployId) {
		Optional<DeployServer> deployServiceOptional = deployServerRepository.findById(deployId);
		if (deployServiceOptional.isPresent()) {
			DeployServer deployServer = deployServiceOptional.get();
			return getBuildInfo(deployServer.getService().getJobName(), deployServer);
		}
		return new JenkinsBuild();
	}

	public String startBuild(Long deployId) throws URISyntaxException {
		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
				jenkinsProperties.getUser(), jenkinsProperties.getPassword());
		Optional<DeployServer> deployServiceOptional = deployServerRepository.findById(deployId);
		if (deployServiceOptional.isPresent()) {
			DeployServer server = deployServiceOptional.get();
			try {
				JobWithDetails jobWithDetails = jenkinsServer.getJob(server.getService().getJobName());
				Map<String, String> param = new HashMap<>(); // service.getMetadata();
				param.put("projectName", server.getService().getProjectName());
				param.put("server", server.getHost());
				QueueReference reference = jobWithDetails.build(param, true);
				String itemUrl = reference.getQueueItemUrlPart();
				server.setQueueId(itemUrl);
				deployServerRepository.save(server);
				return itemUrl;
			}
			catch (IOException ex) {
				LOGGER.error("error push job to jenkins", ex);
			}
		}
		return "";
	}

	public boolean stopBuild(Long deployId) {
		Optional<DeployServer> deployServiceOptional = deployServerRepository.findById(deployId);
		if (deployServiceOptional.isPresent()) {
			DeployServer deployServer = deployServiceOptional.get();
			Pair<Optional<Build>, Boolean> pair = getBuild(deployServer.getService().getJobName(), deployServer);
			if (pair != null) {
				Optional<Build> build = pair.getFirst();
				try {
					if (build.isPresent()) {
						BuildWithDetails buildWithDetails = build.get().details();
						updateDeployBuildId(deployServer, buildWithDetails.getId());
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

	private void updateDeployBuildId(DeployServer deployServer, String buildId) {
		deployServer.setLastBuildId(buildId);
		deployServer.setQueueId("");
		deployServerRepository.save(deployServer);
	}

	public JenkinsBuild getBuildInfo(String jobName, DeployServer deployServer) {
		try {
			Pair<Optional<Build>, Boolean> pair = getBuild(jobName, deployServer);
			if (pair != null) {
				Optional<Build> build = pair.getFirst();
				JenkinsBuild jenkinsBuild;
				if (build.isPresent()) {
					BuildWithDetails buildWithDetails = build.get().details();
					updateDeployBuildId(deployServer, buildWithDetails.getId());

					jenkinsBuild = new JenkinsBuild(pair.getSecond(), buildWithDetails.isBuilding(),
							buildWithDetails.getDuration(), buildWithDetails.getEstimatedDuration(),
							buildWithDetails.getTimestamp());
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

	public Pair<Optional<Build>, Boolean> getBuild(String jobName, DeployServer deployServer) {
		try {
			JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
					jenkinsProperties.getUser(), jenkinsProperties.getPassword());
			JobWithDetails jobWithDetails = jenkinsServer.getJob(jobName);
			Build build = null;
			QueueItem queueItem = null;
			String reference = deployServer.getQueueId();
			if (!StringUtils.isEmpty(reference)) {
				QueueReference queueReference = new QueueReference(reference);
				queueItem = jenkinsServer.getQueueItem(queueReference);

				if (!queueItem.isCancelled() && jobWithDetails.isInQueue()) {
					return Pair.of(Optional.empty(), queueItem != null);
				}
				build = jenkinsServer.getBuild(queueItem);
			}
			else if (!StringUtils.isEmpty(deployServer.getLastBuildId())) {
				build = jobWithDetails.getBuildByNumber(Integer.parseInt(deployServer.getLastBuildId()));
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
		Optional<DeployServer> deployServiceOptional = deployServerRepository.findById(deployId);
		if (deployServiceOptional.isPresent()) {
			try {
				DeployServer deployServer = deployServiceOptional.get();
				Pair<Optional<Build>, Boolean> pair = getBuild(deployServer.getService().getJobName(), deployServer);
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

	public Long addDeploy(DeployRequest deployRequest) {
		Optional<MicroService> microServiceOptional = microServiceRepository.findByName(deployRequest.getName());
		MicroService microService;
		if (microServiceOptional.isPresent()) {
			microService = microServiceOptional.get();
			microService.setJobName(deployRequest.getJobName());
			microService.setProjectName(deployRequest.getProjectName());
		}
		else {
			microService = new MicroService(deployRequest.getName(), deployRequest.getJobName(),
					deployRequest.getProjectName());
		}
		microService = microServiceRepository.save(microService);
		return microService.getId();
	}

	public Long addDeployServer(DeployServerRequest deployServerRequest) {
		Optional<MicroService> microServiceOptional = microServiceRepository
				.findById(deployServerRequest.getServiceId());
		if (microServiceOptional.isPresent()) {
			MicroService microService = microServiceOptional.get();
			DeployServer deployServer = new DeployServer(microService.getId(), deployServerRequest.getHost());
			deployServer = deployServerRepository.save(deployServer);
			return deployServer.getId();
		}
		return 0L;
	}

}
