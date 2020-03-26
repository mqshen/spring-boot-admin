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

@lombok.Data
public class Jenkins {

	private String projectName;

	private String host;

	private String user;

	private String password;

	private String args;

	public Jenkins(String projectName, String host, String user, String password, String args) {
		this.projectName = projectName;
		this.host = host;
		this.user = user;
		this.password = password;
		this.args = args;
	}

}
