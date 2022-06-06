package com.namelessmc.bot.connections;

import com.namelessmc.java_api.NamelessAPI;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class StatelessConnectionManager extends ConnectionManager {

	private final long guildId;
	private final NamelessAPI api;

	public StatelessConnectionManager(final long guildId, final URL apiUrl, String apiKey) {
		Objects.requireNonNull(apiUrl, "Api url is null");
		this.guildId = guildId;
		this.api = ConnectionCache.getApiConnection(apiUrl, apiKey);
	}

	@Override
	public Optional<NamelessAPI> getApiConnection(final long guildId) {
		if (guildId != this.guildId) {
			return Optional.empty();
		} else {
			return Optional.of(this.api);
		}
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void createConnection(final long guildId, final @NotNull URL apiUrl, final @NotNull String apiKey) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public boolean removeConnection(final long guildId) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Optional<Long> getLastUsed(final long guildId) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public int countConnections() {
		return 1;
	}

	@Override
	public @NotNull List<@NotNull NamelessAPI> listConnections() {
		// Optional should always be present, this method should never throw an exception here
		return Collections.singletonList(this.api);
	}

	@Override
	public boolean updateConnection(final long guildId, final @NotNull URL apiUrl, final @NotNull String apiKey) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public @NotNull List<@NotNull NamelessAPI> listConnectionsUsedBefore(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public @NotNull List<@NotNull NamelessAPI> listConnectionsUsedSince(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public @NotNull Optional<@NotNull Long> getGuildIdByApiUrl(final @NotNull URL apiUrl) throws BackendStorageException {
		return this.api.apiUrl().equals(apiUrl) ? Optional.of(this.guildId) : Optional.empty();
	}

}
