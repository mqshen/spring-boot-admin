package de.codecentric.boot.admin.server.config;

import java.util.List;

import lombok.Data;

@Data
public class MicroService {

	private String name;

	private List<String> server;

}
