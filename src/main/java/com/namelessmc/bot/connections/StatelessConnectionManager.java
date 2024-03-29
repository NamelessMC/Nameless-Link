package com.namelessmc.bot.connections;

import com.namelessmc.java_api.NamelessAPI;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.net.URL;
import java.util.*;

public class StatelessConnectionManager extends ConnectionManager {

	private final long guildId;
	private final boolean usernameSyncEnabled;
	private final NamelessAPI api;

	public StatelessConnectionManager(final long guildId, final URL apiUrl, String apiKey, boolean usernameSyncEnabled) {
		Objects.requireNonNull(apiUrl, "Api url is null");
		this.guildId = guildId;
		this.usernameSyncEnabled = usernameSyncEnabled;
		this.api = ConnectionCache.getApiConnection(apiUrl, apiKey);
	}

	@Override
	public @Nullable NamelessAPI getApiConnection(final long guildId) {
		return guildId == this.guildId ? this.api : null;
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void createConnection(final long guildId, final URL apiUrl, final String apiKey) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public boolean removeConnection(final long guildId) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public int countConnections() {
		return 1;
	}

	@Override
	public Collection<NamelessAPI> listConnections() {
		return Collections.singletonList(this.api);
	}

	@Override
	public boolean updateConnection(final long guildId, final URL apiUrl, final String apiKey) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Collection<NamelessAPI> listConnectionsUsedBefore(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Collection<NamelessAPI> listConnectionsUsedSince(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Collection<Long> listGuildsUsernameSyncEnabled() throws BackendStorageException {
		if (this.usernameSyncEnabled) {
			return Collections.singletonList(this.guildId);
		} else {
			return Collections.emptyList();
		}
	}

	public void setUsernameSyncEnabled(long guildId, boolean usernameSyncEnabled) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Optional<Long> getGuildIdByApiUrl(final URL apiUrl) throws BackendStorageException {
		return this.api.apiUrl().equals(apiUrl) ? Optional.of(this.guildId) : Optional.empty();
	}

}
