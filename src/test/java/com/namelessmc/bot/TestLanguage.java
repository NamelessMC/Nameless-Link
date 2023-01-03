package com.namelessmc.bot;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static com.namelessmc.bot.Language.Term.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TestLanguage {

	private Language getBaseLanguage() throws Language.LanguageLoadException {
		return getLanguage("en_UK");
	}

	private Language getLanguage(String languageCode) throws Language.LanguageLoadException {
		Language.setDefaultLanguage(languageCode);
		return Language.getDefaultLanguage();
	}

	private List<String> getLanguageCodes() throws IOException {
		Path languagesDir = Path.of(".", "src", "main", "resources", "languages");
		return Files.list(languagesDir).map(p -> {
			String fileName = p.getFileName().toString();
			return fileName.substring(0, fileName.length() - 5);
		}).toList();
	}

	private Object[] getFakeReplacements(Language.Term term) {
		String[] placeholders = term.getPlaceholders();
		Object[] replacements = new Object[placeholders.length * 2];
		for (int i = 0; i < placeholders.length; i++) {
			replacements[i*2] = placeholders[i];
			replacements[i*2+1] = "test placeholder value";
		}
		return replacements;
	}

	@Test
	void baseLanguageHasAllTerms() throws Language.LanguageLoadException {
		Language language = getBaseLanguage();
		for (Language.Term term : Language.Term.values()) {
			language.get(term, getFakeReplacements(term));
		}
		assertTrue(true);
	}

	private static final Set<Language.Term> MAX_100_CHARS = EnumSet.of(
			CONFIGURE_DESCRIPTION,
			CONFIGURE_LINK_DESCRIPTION,
			CONFIGURE_LINK_OPTION_API_URL,
			CONFIGURE_LINK_OPTION_API_KEY,
			CONFIGURE_UNLINK_DESCRIPTION,
			CONFIGURE_TEST_DESCRIPTION,
			CONFIGURE_USERNAME_SYNC_DESCRIPTION,
			CONFIGURE_USERNAME_SYNC_OPTION_STATE,
			CONFIGURE_UPDATE_USERNAMES_DESCRIPTION,
			VERIFY_DESCRIPTION,
			VERIFY_OPTION_TOKEN,
			APIURL_DESCRIPTION,
			APIURL_OPTION_APIKEY,
			APIURL_OPTION_URL,
			PING_DESCRIPTION,
			REGISTER_DESCRIPTION,
			REGISTER_OPTION_EMAIL,
			REGISTER_OPTION_USERNAME
	);

	private int maxLength(Language.Term term) {
		if (MAX_100_CHARS.contains(term)) {
			return 100;
		} else {
			return Integer.MAX_VALUE;
		}
	}

	@Test
	void testCommandUsageLength() throws IOException, Language.LanguageLoadException {
		for (String languageCode : getLanguageCodes()) {
			Language language = getLanguage(languageCode);
			for (Language.Term term : Language.Term.values()) {
				try {
					int length = language.get(term, getFakeReplacements(term)).length();
					assertTrue(length < maxLength(term), "Term " + term.name() + " too long in language " + languageCode);
				} catch (Language.MissingTermException ignored) {}
			}
		}
	}

}
