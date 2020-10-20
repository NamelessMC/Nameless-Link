package com.namelessmc.bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor(access = AccessLevel.PUBLIC)
public class ConnectionManager {
	
	@Getter
	private final Optional<File> file;
	private final Map<Long, WebsiteConnection> connections = new HashMap<>();

	public void load() throws FileNotFoundException, IOException {
		if (this.file.isEmpty()) {
			throw new IllegalStateException("Cannot load from file when this ConnectionManager was not configured to use files");
		}
		
		this.connections.clear();
		try (Reader reader = new FileReader(this.file.get())) {
			final JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
			array.forEach(e -> {
				final JsonObject o = e.getAsJsonObject();
				final String apiUrl = o.get("api-url").getAsString();
				final long guildId = o.get("discord-guild-id").getAsLong();
				final long lastUsedDate = o.get("last-used-date").getAsLong();
				final long setupDate = o.get("setup-date").getAsLong();
				this.connections.put(guildId, new WebsiteConnection(apiUrl, guildId, lastUsedDate, setupDate));
			});
		}
	}
	
	public void save() throws IOException {
		if (this.file.isEmpty()) {
			throw new IllegalStateException("Cannot save to file when this ConnectionManager was not configured to use files");
		}
		
		try (Writer writer = new FileWriter(this.file.get());
				JsonWriter json = new JsonWriter(writer)) {
			json.beginArray();
			for (final WebsiteConnection conn : this.connections.values()) {
				json.beginObject();
				json.name("api-url");
				json.value(conn.getApiUrl());
				json.name("discord-guild-id");
				json.value(conn.getGuildId());
				json.name("last-used-date");
				json.value(conn.getLastUsedDate());
				json.name("setup-date");
				json.value(conn.getSetupDate());
				json.endObject();
			};
			json.endArray();
		}
	}
	
	public boolean isFileBased() {
		return this.file.isPresent();
	}
	
	public WebsiteConnection createNewConnection(final String apiUrl, final long guildId) {
		final WebsiteConnection connection = new WebsiteConnection(apiUrl, guildId, System.currentTimeMillis(), System.currentTimeMillis());
		this.connections.put(guildId, connection);
		return connection;
	}
	
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	public class WebsiteConnection {
		
		private final String apiUrl;
		private final long guildId;
		@Getter
		private long lastUsedDate;
		@Getter
		private final long setupDate;
		
		public String getApiUrl() {
			this.lastUsedDate = System.currentTimeMillis();
			return this.apiUrl;
		}
		
		public long getGuildId() {
			this.lastUsedDate = System.currentTimeMillis();
			return this.guildId;
		}

	}
	
}
