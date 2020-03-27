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

package de.codecentric.boot.admin.server.web;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.BuildWithDetails;
import com.offbytwo.jenkins.model.JobWithDetails;
import com.offbytwo.jenkins.model.QueueItem;
import com.offbytwo.jenkins.model.QueueReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import de.codecentric.boot.admin.server.domain.values.Jenkins;
import de.codecentric.boot.admin.server.domain.values.JenkinsBuild;

@AdminController
@ResponseBody
@RequestMapping("/jenkins")
public class JenkinsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(InstancesController.class);

	@PostMapping(path = "/build")
	public ResponseEntity<String> build(@RequestBody Jenkins jenkins) throws URISyntaxException {
		LOGGER.debug("start jenkins build");
		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkins.getHost()), jenkins.getUser(),
				jenkins.getPassword());
		try {
			JobWithDetails jobWithDetails = jenkinsServer.getJob(jenkins.getProjectName());
			Map<String, String> param = new HashMap<>();
			String args = jenkins.getArgs();
			if (args != null && !args.isEmpty()) {
				for (String item : args.split("&")) {
					String[] pair = item.split("=");
					if (pair.length == 2) {
						param.put(pair[0], pair[1]);
					}
				}
			}

			QueueReference reference = jobWithDetails.build(param);
			String itemUrl = reference.getQueueItemUrlPart();
			return ResponseEntity.ok(itemUrl);
		}
		catch (IOException ex) {
			LOGGER.error("build jenkins failed ", ex);
		}
		return ResponseEntity.badRequest().body("project not found");
	}

	@PostMapping(path = "/detail")
	public ResponseEntity<JenkinsBuild> detail(@RequestBody Jenkins jenkins) throws URISyntaxException {
		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkins.getHost()), jenkins.getUser(),
				jenkins.getPassword());
		try {
			JobWithDetails jobWithDetails = jenkinsServer.getJob(jenkins.getProjectName());
			BuildWithDetails buildWithDetails = jobWithDetails.getLastBuild().details();
			QueueItem queueItem = jobWithDetails.getQueueItem();
			JenkinsBuild jenkinsBuild = new JenkinsBuild(queueItem != null, buildWithDetails.isBuilding(),
				buildWithDetails.getDuration(), buildWithDetails.getEstimatedDuration(),
				buildWithDetails.getTimestamp());
			return ResponseEntity.ok(jenkinsBuild);
		}
		catch (IOException ex) {
			LOGGER.error("query jenkins failed ", ex);
		}
		return ResponseEntity.badRequest().build();
	}

}
