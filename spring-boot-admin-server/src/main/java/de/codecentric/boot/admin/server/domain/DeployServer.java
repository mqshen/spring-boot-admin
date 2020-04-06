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

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@NoArgsConstructor
@Table(name = "ifp_server")
public class DeployServer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Long id;

	Long environmentId;

	String name;

	String ip;

	@Enumerated(EnumType.ORDINAL)
	LoginType loginType;

	String user;

	String password;

	public DeployServer(Long environmentId, String name, String ip, LoginType loginType, String user, String password) {
		this.environmentId = environmentId;
		this.name = name;
		this.ip = ip;
		this.loginType = loginType;
		this.user = user;
		this.password = password;
	}

	// @ManyToOne(fetch = FetchType.LAZY, targetEntity = Environment.class)
	// @JoinColumn(name = "environmentId", insertable = false, updatable = false)
	// private Environment environment;

	// @OneToMany(fetch = FetchType.LAZY, targetEntity = DeployInstance.class)
	// @JoinColumn(name = "serverId", insertable = false, updatable = false)
	// private List<DeployInstance> instances;

}
