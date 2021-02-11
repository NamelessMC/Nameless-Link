package com.namelessmc.bot.connections;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class PostgresConnectionManager extends JDBCConnectionManager {

	private final String hostname, databaseName, username, password;
	private final int port;

	public PostgresConnectionManager(final String hostname, final int port, final String databaseName,
									 final String username, final String password) {
		this.hostname = hostname;
		this.port = port;
		this.databaseName = databaseName;
		this.username = username;
		this.password = password;
	}

	@Override
	public Connection getNewDatabaseConnection() throws SQLException {
		return DriverManager.getConnection(
				String.format("jdbc:postgresql://%s:%s/%s", this.hostname, this.port, this.databaseName), this.username,
				this.password);
	}

}
