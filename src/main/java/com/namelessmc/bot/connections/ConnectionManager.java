package com.namelessmc.bot.connections;

import com.namelessmc.java_api.NamelessAPI;
import org.jetbrains.annotations.NotNull;

import java.net.URL;
import java.util.List;
import java.util.Optional;

public abstract class ConnectionManager {

	public abstract boolean isReadOnly();

	public abstract Optional<NamelessAPI> getApiConnection(long guildId) throws BackendStorageException;

	public abstract void createConnection(long guildId, URL apiUrl, String apiKey) throws BackendStorageException;

	public abstract boolean updateConnection(long guildId, URL apiUrl, String apiKey) throws BackendStorageException;

	public abstract boolean removeConnection(long guildId) throws BackendStorageException;

	public abstract int countConnections() throws BackendStorageException;

	public abstract @NotNull List<@NotNull NamelessAPI> listConnections() throws BackendStorageException;

	public abstract @NotNull List<@NotNull NamelessAPI> listConnectionsUsedBefore(long time) throws BackendStorageException;

	public abstract @NotNull List<@NotNull NamelessAPI> listConnectionsUsedSince(long time) throws BackendStorageException;

	public abstract Optional<Long> getLastUsed(long guildId) throws BackendStorageException;

	public abstract Optional<Long> getGuildIdByApiUrl(URL apiUrl) throws BackendStorageException;

	public Optional<Long> getGuildIdByApiConnection(NamelessAPI api) throws BackendStorageException {
		return this.getGuildIdByApiUrl(api.apiUrl());
	}

}
