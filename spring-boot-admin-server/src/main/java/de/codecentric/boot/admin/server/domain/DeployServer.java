package de.codecentric.boot.admin.server.domain;


import java.util.List;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
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

	//	@ManyToOne(fetch = FetchType.LAZY, targetEntity = Environment.class)
//	@JoinColumn(name = "environmentId", insertable = false, updatable = false)
//	private Environment environment;

//	@OneToMany(fetch = FetchType.LAZY, targetEntity = DeployInstance.class)
//	@JoinColumn(name = "serverId", insertable = false, updatable = false)
//	private List<DeployInstance> instances;
}
