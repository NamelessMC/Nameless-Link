package com.namelessmc.bot.connections;

import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ConnectionCache {

	private record CacheKey(URL apiUrl, String apiKey) {}

	private static final Map<CacheKey, NamelessAPI> API_CACHE = new HashMap<>();

	public static NamelessAPI getApiConnection(final URL apiUrl, final String apiKey) {
		final CacheKey cacheKey = new CacheKey(apiUrl, apiKey);
		synchronized (API_CACHE) {
			return API_CACHE.computeIfAbsent(cacheKey, x ->
					NamelessAPI.builder(apiUrl, apiKey)
							.customDebugLogger(Main.getApiDebugLogger())
							.userAgent(Main.USER_AGENT)
							.build());
		}
	}

}
