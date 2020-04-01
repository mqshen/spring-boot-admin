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

import lombok.AllArgsConstructor;

@lombok.Data
@AllArgsConstructor
public class JenkinsBuild {

	private boolean queued;

	private boolean building;

	private long duration;

	private long estimatedDuration;

	private long timestamp;

	public JenkinsBuild() {
		this.queued = false;
		this.building = false;
		duration = 0;
		estimatedDuration = 0;
		timestamp = 0;
	}

	public JenkinsBuild(boolean queued, boolean building) {
		this.queued = queued;
		this.building = building;
	}

}
