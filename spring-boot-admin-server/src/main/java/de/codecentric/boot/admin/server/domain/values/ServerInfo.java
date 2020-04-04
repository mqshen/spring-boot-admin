package de.codecentric.boot.admin.server.domain.values;


import java.util.List;

import de.codecentric.boot.admin.server.domain.DeployServer;
import de.codecentric.boot.admin.server.domain.Environment;
import de.codecentric.boot.admin.server.domain.LoginType;
import de.codecentric.boot.admin.server.domain.entities.DeployInstanceInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

	public ServerInfo(Long id, EnvironmentInfo environment, String name, String ip, LoginType loginType, String user) {
		this.id = id;
		this.environment = environment;
		this.name = name;
		this.ip = ip;
		this.loginType = loginType;
		this.user = user;
	}

	static public ServerInfo fromEntity(DeployServer server, Environment environment) {
		return new ServerInfo(server.getId(),
			new EnvironmentInfo(server.getEnvironmentId(), environment.getName()),
			server.getName(),
			server.getIp(),
			server.getLoginType(),
			server.getUser());
	}
}
