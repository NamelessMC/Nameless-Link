package com.namelessmc.bot.connections;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;

public class StatelessConnectionManager extends ConnectionManager {

	private final long guildId;
	// Keep one instance for performance
	private final Optional<Long> optGuildId;
	private final Optional<NamelessAPI> api;

	public StatelessConnectionManager(final long guildId, final URL apiUrl) {
		Validate.notNull(apiUrl, "API URL not specified");
		this.guildId = guildId;
		this.optGuildId = Optional.of(guildId);
		this.api = Optional.of(Main.newApiConnection(apiUrl));
	}

	@Override
	public Optional<NamelessAPI> getApi(final long guildId) {
		if (guildId != this.guildId) {
			return Optional.empty();
		} else {
			return this.api;
		}
	}

	@Override
	public boolean isReadOnly() {
		return true;
	}

	@Override
	public void newConnection(final long guildId, final URL apiUrl) throws BackendStorageException {
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
	public List<URL> listConnections() {
		// Optional should always be present, this method should never throw an exception here
		return Collections.singletonList(this.api.orElseThrow().getApiUrl());
	}

	@Override
	public boolean updateConnection(final long guildId, final URL apiUrl) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public List<URL> listConnectionsUsedBefore(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public List<URL> listConnectionsUsedSince(final long time) throws BackendStorageException {
		throw new BackendStorageException(new UnsupportedOperationException());
	}

	@Override
	public Optional<Long> getGuildIdByURL(final URL url) throws BackendStorageException {
		// Optional should always be present, this method should never throw an exception here
		if (url == this.api.orElseThrow().getApiUrl()) {
			return this.optGuildId;
		} else {
			throw new BackendStorageException(new UnsupportedOperationException());
		}
	}

}
