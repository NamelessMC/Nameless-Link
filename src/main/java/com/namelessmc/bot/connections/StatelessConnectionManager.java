package com.namelessmc.bot.connections;

import java.net.URL;
import java.util.Optional;

import org.apache.commons.lang3.Validate;

import com.namelessmc.java_api.NamelessAPI;

public class StatelessConnectionManager extends ConnectionManager {

	
	private final long guildId;
	private final Optional<NamelessAPI> api; // Keep one instance for performance
	
	public StatelessConnectionManager(final long guildId, final URL apiUrl) {
		Validate.notNull(guildId, "Guild ID not specified");
		Validate.notNull(apiUrl, "API URL not specified");
		this.guildId = guildId;
		this.api = Optional.of(new NamelessAPI(apiUrl));
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
}
