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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.codecentric.boot.admin.server.config.JenkinsProperties;
import de.codecentric.boot.admin.server.domain.DeployInstance;
import de.codecentric.boot.admin.server.domain.InstanceStatus;
import de.codecentric.boot.admin.server.domain.MicroService;
import de.codecentric.boot.admin.server.domain.entities.Instance;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;
import de.codecentric.boot.admin.server.repositories.DeployInstanceRepository;

@Service
public class JenkinsService {

	private static final Logger LOGGER = LoggerFactory.getLogger(JenkinsService.class);

	private final CopyOnWriteArrayList<Long> listenerJenkins;

	@Autowired
	private DeployInstanceRepository deployInstanceRepository;

	@Autowired
	private ApplicationRegistry registry;

	@Autowired
	private JenkinsProperties jenkinsProperties;

	@Autowired
	private DeployService deployService;

	@Value("${eureka.client.serviceUrl.defaultZone}")
	private String eurekaAddress;

	private JenkinsPublisher jenkinsPublisher;

	public JenkinsService() {
		this.jenkinsPublisher = new JenkinsPublisher();
		this.listenerJenkins = new CopyOnWriteArrayList<>();
	}

	public void startListener(Long deployId) {
		listenerJenkins.add(deployId);
	}

	public void startListener(List<Long> instances) {
		listenerJenkins.addAll(instances);
	}

	public void stopListener(Long deployId) {
		listenerJenkins.remove(deployId);
	}

	@Scheduled(fixedRate = 5000)
	public void syncJenkinsState() {
		try {
			listenerJenkins.stream().forEach((deployId) -> {
				DeployInstance deployInstance = deployService.getDeployInstance(deployId);
				if (InstanceStatus.SHUTDOWN.equals(deployInstance.getStatus())) {
					Instance instance = deployService.getInstance(deployId);
					if (instance == null || !instance.getStatusInfo().isUp()) {
						deployInstance.setStatus(InstanceStatus.ENDED);
					}
				}
				else {
					MicroService microService = deployService.getMicroService(deployInstance.getServiceId());
					getBuild(microService.getJobName(), deployInstance);
				}
				if (InstanceStatus.ENDED.equals(deployInstance.getStatus())) {
					jenkinsPublisher.append(deployInstance);
				}
			});
		}
		catch (Exception ex) {
			LOGGER.error("failed scheduleFixedRateTask task", ex);
		}
	}

	public JenkinsPublisher getJenkinsPublisher() {
		return jenkinsPublisher;
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
				}
				else {
					jenkinsBuild = new JenkinsBuild(pair.getSecond(), false);
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

				if (jobWithDetails.isInQueue() && !queueItem.isCancelled()) {
					return Pair.of(Optional.empty(), queueItem != null);
				}
				if (queueItem != null) {
					build = jenkinsServer.getBuild(queueItem);
				}
			}
			else if (!StringUtils.isEmpty(deployInstance.getLastBuildId())) {
				build = jobWithDetails.getBuildByNumber(Integer.parseInt(deployInstance.getLastBuildId()));
			}
			if (build == null) {
				deployInstance.setStatus(InstanceStatus.ENDED);
				return Pair.of(Optional.empty(), queueItem != null);
			}
			else {
				if (!build.details().isBuilding() && queueItem == null) {
					deployInstance.setStatus(InstanceStatus.ENDED);
					stopListener(deployInstance.getId());
				}
				updateDeployBuildId(deployInstance, build.details().getId());
				return Pair.of(Optional.of(build), queueItem != null);
			}
		}
		catch (Exception ex) {
			LOGGER.error("query jenkins failed ", ex);
		}
		return null;
	}

	private void updateDeployBuildId(DeployInstance deployInstance, String buildId) {
		if (deployInstance.getLastBuildId() != null && !buildId.equals(deployInstance.getLastBuildId())) {
			deployInstance.setLastBuildId(buildId);
			deployInstance.setQueueId("");
			deployInstanceRepository.save(deployInstance);
		}
	}

	public boolean stopBuild(String jobName, DeployInstance deployInstance) {
		Pair<Optional<Build>, Boolean> pair = getBuild(jobName, deployInstance);
		if (pair != null) {
			Optional<Build> build = pair.getFirst();
			try {
				if (build.isPresent()) {
					build.get().Stop(true);
					return true;
				}
			}
			catch (IOException ex) {
				LOGGER.error("error push job to jenkins", ex);
			}
		}
		return false;
	}

	public String sendBuild(String jobName, Map<String, String> param) throws IOException, URISyntaxException {
		if (eurekaAddress != null) {
			int index = eurekaAddress.indexOf("/eureka");
			String eurekaUrl = eurekaAddress;
			if (index > 0) {
				eurekaUrl = eurekaAddress.substring(0, index);
			}
			param.put("eurekaAddress", eurekaUrl);
		}
		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkinsProperties.getHost()),
				jenkinsProperties.getUser(), jenkinsProperties.getPassword());

		JobWithDetails jobWithDetails = jenkinsServer.getJob(jobName);
		QueueReference reference = jobWithDetails.build(param, true);
		return reference.getQueueItemUrlPart();
	}

}
