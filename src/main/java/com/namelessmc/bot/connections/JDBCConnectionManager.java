package com.namelessmc.bot.connections;

import com.namelessmc.java_api.NamelessAPI;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public abstract class JDBCConnectionManager extends ConnectionManager {

	private static final Logger LOGGER = LoggerFactory.getLogger("JDBC Connection Manager");

	public abstract Connection getNewDatabaseConnection() throws SQLException;

	@Override
	public boolean isReadOnly() {
		return false;
	}

	@Override
	public Optional<NamelessAPI> getApiConnection(final long guildId) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			String apiUrl, apiKey;
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT api_url, api_key FROM connections WHERE guild_id=?")) {
				statement.setLong(1, guildId);
				final ResultSet result = statement.executeQuery();
				if (!result.next()) {
					return Optional.empty();
				}
				apiUrl = result.getString(1);
				apiKey = result.getString(2);
			}

			try (PreparedStatement statement = connection
					.prepareStatement("UPDATE connections SET last_use=? WHERE guild_id=?")) {
				statement.setLong(1, System.currentTimeMillis());
				statement.setLong(2, guildId);
				statement.executeUpdate();
			}

			return Optional.of(ConnectionCache.getApiConnection(new URL(apiUrl), apiKey));
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		} catch (final MalformedURLException e) {
			// This should never happen since malformed URLs are not allowed in the database
			// Pretend as if the website was not set up
			e.printStackTrace();
			return Optional.empty();
		}
	}

	@Override
	public void createConnection(final long guildId, final URL apiUrl, String apiKey) throws BackendStorageException {
		Objects.requireNonNull(apiUrl, "Api url is null");
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("INSERT INTO connections (guild_id, api_url, api_key, last_use) VALUES (?, ?, ?, ?)")) {
				statement.setLong(1, guildId);
				statement.setString(2, apiUrl.toString());
				statement.setString(3, apiKey);
				statement.setLong(4, System.currentTimeMillis());
				statement.execute();
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public boolean updateConnection(final long guildId, final URL apiUrl, String apiKey) throws BackendStorageException {
		Objects.requireNonNull(apiUrl, "Api url is null");
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("UPDATE connections SET api_url=?, api_key=?, last_use=? WHERE guild_id=?")) {
				statement.setString(1, apiUrl.toString());
				statement.setString(2, apiKey);
				statement.setLong(3, System.currentTimeMillis());
				statement.setLong(4, guildId);
				return statement.executeUpdate() > 0;
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public boolean removeConnection(final long guildId) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("DELETE FROM connections WHERE guild_id=?")) {
				statement.setLong(1, guildId);
				return statement.execute();
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public int countConnections() throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection();
				PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM connections")) {
			final ResultSet result = statement.executeQuery();
			result.next();
			return result.getInt(1);
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	private List<NamelessAPI> listConnectionsQuery(final String query, final Long optLong) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection.prepareStatement(query)) {
				if (optLong != null) {
					statement.setLong(1, optLong);
				}
				final ResultSet result = statement.executeQuery();
				final List<NamelessAPI> connections = new ArrayList<>();
				while (result.next()) {
					try {
						NamelessAPI apiConnection = ConnectionCache.getApiConnection(
								new URL(result.getString("api_url")),
								result.getString("api_key"));
						connections.add(apiConnection);
					} catch (final MalformedURLException e) {
						LOGGER.warn("Skipped invalid URL in listConnections(): " + result.getString("api_url"));
						e.printStackTrace();
					}
				}
				return connections;
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public @NotNull List<@NotNull NamelessAPI> listConnections() throws BackendStorageException {
		return listConnectionsQuery("SELECT api_url, api_key FROM connections", null);
	}

	@Override
	public @NotNull List<@NotNull NamelessAPI> listConnectionsUsedSince(final long time) throws BackendStorageException {
		return listConnectionsQuery("SELECT api_url, api_key FROM connections WHERE last_use > ?", time);
	}

	@Override
	public @NotNull List<@NotNull NamelessAPI> listConnectionsUsedBefore(final long time) throws BackendStorageException {
		return listConnectionsQuery("SELECT api_url, api_key FROM connections WHERE last_use < ?", time);
	}

	@Override
	public Optional<Long> getLastUsed(final long guildId) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT api_url FROM connections WHERE guild_id=?")) {
				statement.setLong(1, guildId);
				final ResultSet result = statement.executeQuery();
				if (!result.next()) {
					return Optional.empty();
				}
				return Optional.of(result.getLong(1));
			}
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

	@Override
	public Optional<Long> getGuildIdByApiUrl(final @NotNull URL apiUrl) throws BackendStorageException {
		try (Connection connection = this.getNewDatabaseConnection()) {
			long guildId;
			try (PreparedStatement statement = connection
					.prepareStatement("SELECT guild_id FROM connections WHERE api_url=?")) {
				statement.setString(1, apiUrl.toString());
				final ResultSet result = statement.executeQuery();
				if (!result.next()) {
					return Optional.empty();
				}
				guildId = result.getLong(1);
			}

			return Optional.of(guildId);
		} catch (final SQLException e) {
			throw new BackendStorageException(e);
		}
	}

}
