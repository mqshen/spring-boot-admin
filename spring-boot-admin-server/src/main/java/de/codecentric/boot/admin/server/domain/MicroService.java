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

package de.codecentric.boot.admin.server.domain;

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
@Table(name = "ifp_micro_service")
public class MicroService {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	String name;

	String jobName;

	String projectName;

	@Enumerated(EnumType.ORDINAL)
	DeployType deployType;

	boolean autoStart;

	String branch;

	String rollbackBranch;

	String profile;

	int port;

	String path;

	String env;

	String parameter;

	public MicroService(String name, String jobName, String projectName, DeployType deployType, boolean autoStart,
			String branch, String rollbackBranch, String profile, int port, String path, String env, String parameter) {
		this.name = name;
		this.jobName = jobName;
		this.projectName = projectName;
		this.deployType = deployType;
		this.autoStart = autoStart;
		this.branch = branch;
		this.rollbackBranch = rollbackBranch;
		this.profile = profile;
		this.port = port;
		this.path = path;
		this.env = env;
		this.parameter = parameter;
	}

}
