package de.codecentric.boot.admin.server.web;

import com.offbytwo.jenkins.JenkinsServer;
import com.offbytwo.jenkins.model.JobWithDetails;
import de.codecentric.boot.admin.server.domain.values.Jenkins;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

@AdminController
@ResponseBody
public class JenkinsController {
	private static final Logger LOGGER = LoggerFactory.getLogger(InstancesController.class);

	@PostMapping(path = "/buildJenkins")
	public String register(@RequestBody Jenkins jenkins) throws URISyntaxException {
		LOGGER.error("start jenkins build");
		JenkinsServer jenkinsServer = new JenkinsServer(new URI(jenkins.getHost()),
			jenkins.getUser(), jenkins.getPassword());
		try {
			JobWithDetails jobWithDetails = jenkinsServer.getJob(jenkins.getProjectName());
			jobWithDetails.build(new HashMap<>());
		} catch (IOException e) {
			LOGGER.error("build jenkins failed ", e);
		}
		return "SUCCESS";
	}

}
