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

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.UnicastProcessor;

public class JenkinsPublisher implements Publisher<Long> {

	private static final Logger log = LoggerFactory.getLogger(JenkinsPublisher.class);

	private final Flux<Long> publishedFlux;

	private final FluxSink<Long> sink;

	public JenkinsPublisher() {
		UnicastProcessor<Long> unicastProcessor = UnicastProcessor.create();
		this.publishedFlux = unicastProcessor.publish().autoConnect(0);
		this.sink = unicastProcessor.sink();
	}

	protected void publish(Long event) {
		log.debug("Event published {}", event);
		this.sink.next(event);
	}

	public boolean append(Long event) {
		if (event == null) {
			return true;
		}
		publish(event);
		return true;
	}

	@Override
	public void subscribe(Subscriber<? super Long> s) {
		publishedFlux.subscribe(s);
	}

}
