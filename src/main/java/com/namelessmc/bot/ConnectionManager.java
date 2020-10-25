package com.namelessmc.bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonWriter;
import com.namelessmc.java_api.NamelessAPI;

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
				final long guildId = o.get("discord-guild-id").getAsLong();
				final long lastUsedDate = o.get("last-used-date").getAsLong();
				final long setupDate = o.get("setup-date").getAsLong();
				final String apiUrl = o.get("api-url").getAsString();
				URL url;
				try {
					url = new URL(apiUrl);
				} catch (final MalformedURLException e1) {
					System.err.println("Ignoring guild with malformed URL " + guildId);
					return;
				}
				this.connections.put(guildId, new WebsiteConnection(url, guildId, lastUsedDate, setupDate));
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
				json.value(conn.getApiUrl().toString());
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
	
	public WebsiteConnection createNewConnection(final URL apiUrl, final long guildId) {
		final WebsiteConnection connection = new WebsiteConnection(apiUrl, guildId, System.currentTimeMillis(), System.currentTimeMillis());
		this.connections.put(guildId, connection);
		return connection;
	}
	
	public Optional<WebsiteConnection> getConnection(final long discordGuildId) {
		return Optional.ofNullable(this.connections.get(discordGuildId));
	}
	
	public Optional<NamelessAPI> getApi(final long discordGuildId) {
		final Optional<WebsiteConnection> connection = getConnection(discordGuildId);
		if (connection.isEmpty()) {
			return Optional.empty();
		}
		
		// TODO Make url not nullable
		final URL url = connection.get().getApiUrl();
		if (url == null) {
			return Optional.empty();
		}
		
		return Optional.of(new NamelessAPI(url));
	}
	
	@AllArgsConstructor(access = AccessLevel.PROTECTED)
	public class WebsiteConnection {
		
		private final URL apiUrl;
		private final long guildId;
		@Getter
		private long lastUsedDate;
		@Getter
		private final long setupDate;
		
		public URL getApiUrl() {
			this.lastUsedDate = System.currentTimeMillis();
			return this.apiUrl;
		}
		
		public long getGuildId() {
			this.lastUsedDate = System.currentTimeMillis();
			return this.guildId;
		}

	}
	
}
