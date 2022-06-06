package com.namelessmc.bot;

import com.github.mizosoft.methanol.Methanol;
import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class Metrics {

	private static final URI SUBMIT_URI = URI.create("https://nameless-metrics.rkslot.nl/submit");
	private static final String SOURCE = "nameless-link";
	private static final String USER_AGENT = "Nameless-Link";
	private static final String METRICS_ID = UUID.randomUUID().toString();

	private final Methanol methanol;

	Metrics() {
		this.methanol = Methanol.create();

		Main.getExecutorService().scheduleAtFixedRate(this::send, 1, 5, TimeUnit.MINUTES);
	}

	private void send() {
		JsonObject json = new JsonObject();
		json.addProperty("uuid", METRICS_ID);;
		json.addProperty("source", SOURCE);

		JsonObject fields = new JsonObject();
		fields.addProperty("read_only", Main.getConnectionManager().isReadOnly());

		json.add("fields", fields);

		HttpRequest request = HttpRequest.newBuilder(SUBMIT_URI)
				.header("Content-Type", "application/json")
				.header("User-Agent", USER_AGENT)
				.timeout(Duration.ofSeconds(5))
				.POST(HttpRequest.BodyPublishers.ofString(json.toString(), StandardCharsets.UTF_8))
				.build();

		try {
			this.methanol.send(request, HttpResponse.BodyHandlers.discarding());
		} catch (Exception ignored) {}
	}

}
