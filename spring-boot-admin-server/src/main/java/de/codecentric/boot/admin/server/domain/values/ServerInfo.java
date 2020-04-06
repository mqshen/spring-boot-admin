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

package de.codecentric.boot.admin.server.domain.values;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import de.codecentric.boot.admin.server.domain.DeployServer;
import de.codecentric.boot.admin.server.domain.Environment;
import de.codecentric.boot.admin.server.domain.LoginType;
import de.codecentric.boot.admin.server.domain.entities.DeployInstanceInfo;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerInfo {

	Long id;

	EnvironmentInfo environment;

	String name;

	String ip;

	LoginType loginType;

	String user;

	List<DeployInstanceInfo> instances;

	public ServerInfo(Long id, String name, String ip) {
		this.id = id;
		this.name = name;
		this.ip = ip;
	}

	public ServerInfo(Long id, EnvironmentInfo environment, String name, String ip, LoginType loginType, String user) {
		this.id = id;
		this.environment = environment;
		this.name = name;
		this.ip = ip;
		this.loginType = loginType;
		this.user = user;
	}

	public static ServerInfo fromEntity(DeployServer server, Environment environment) {
		return new ServerInfo(server.getId(), new EnvironmentInfo(server.getEnvironmentId(), environment.getName()),
				server.getName(), server.getIp(), server.getLoginType(), server.getUser());
	}

}
