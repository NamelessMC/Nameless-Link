package com.namelessmc.bot.connections;

import com.google.common.base.Objects;
import com.namelessmc.bot.Main;
import com.namelessmc.java_api.NamelessAPI;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

public class ConnectionCache {

	// TODO convert to record when maven shape plugin supports it
	private static class CacheKey {

		private final @NonNull URL apiUrl;
		private final @NonNull String apiKey;

		CacheKey(final @NonNull URL apiUrl, final @NonNull String apiKey) {
			this.apiKey = apiKey;
			this.apiUrl = apiUrl;
		}

		@Override
		public boolean equals(Object other) {
			if (other instanceof CacheKey otherKey) {
				return this.apiUrl.equals(otherKey.apiUrl) &&
						this.apiKey.equals(otherKey.apiKey);
			}

			return false;
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(this.apiKey, this.apiUrl);
		}

	}

	private static final Map<CacheKey, NamelessAPI> API_CACHE = new HashMap<>();

	public static NamelessAPI getApiConnection(final @NonNull URL apiUrl, final @NonNull String apiKey) {
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
