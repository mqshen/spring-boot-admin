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
public class JenkinsBuild {

	private boolean queued;

	private boolean building;

	private long duration;

	private long estimatedDuration;

	public JenkinsBuild(boolean queued, boolean building, long duration, long estimatedDuration) {
		this.queued = queued;
		this.building = building;
		this.duration = duration;
		this.estimatedDuration = estimatedDuration;
	}

}
