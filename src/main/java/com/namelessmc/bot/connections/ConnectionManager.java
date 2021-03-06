package com.namelessmc.bot.connections;

import com.namelessmc.java_api.NamelessAPI;

import java.net.URL;
import java.util.List;
import java.util.Optional;

public abstract class ConnectionManager {

	public abstract boolean isReadOnly();

	public abstract Optional<NamelessAPI> getApi(long guildId) throws BackendStorageException;

	public abstract void newConnection(long guildId, URL apiUrl) throws BackendStorageException;

	public abstract boolean updateConnection(long guildId, URL apiUrl) throws BackendStorageException;

	public abstract boolean removeConnection(long guildId) throws BackendStorageException;

	public abstract List<URL> listConnections() throws BackendStorageException;

	public abstract Optional<Long> getLastUsed(long guildId) throws BackendStorageException;

	public abstract List<URL> listConnectionsUsedBefore(long time) throws BackendStorageException;

	public abstract List<URL> listConnectionsUsedSince(long time) throws BackendStorageException;

	public abstract Optional<Long> getGuildIdByURL(URL url) throws BackendStorageException;

	public abstract Optional<String> getCommandPrefixByGuildId(long guildId) throws BackendStorageException;

	public abstract boolean setCommandPrefix(long guildId, Optional<String> newPrefix) throws BackendStorageException;

}