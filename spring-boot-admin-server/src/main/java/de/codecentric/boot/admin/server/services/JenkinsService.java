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

import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

public class JenkinsService {

	private static final Logger log = LoggerFactory.getLogger(JenkinsService.class);

	private final CopyOnWriteArrayList<Long> listenerJenkins;

	@Autowired
	private ApplicationRegistry registry;

	private JenkinsPublisher jenkinsPublisher;

	public JenkinsService() {
		this.jenkinsPublisher = new JenkinsPublisher();
		this.listenerJenkins = new CopyOnWriteArrayList<>();
	}

	public void startListener(Long deployId) {
		listenerJenkins.add(deployId);
	}

	public void stopListener(Long deployId) {
		listenerJenkins.remove(deployId);
	}

	@Scheduled(fixedRate = 10000)
	public void syncJenkinsState() {
		try {
			listenerJenkins.stream().forEach((deployId) -> jenkinsPublisher.append(deployId));
		}
		catch (Exception ex) {
			log.error("failed scheduleFixedRateTask task", ex);
		}
	}

	public JenkinsPublisher getJenkinsPublisher() {
		return jenkinsPublisher;
	}

}
