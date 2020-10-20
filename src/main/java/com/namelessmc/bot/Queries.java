package com.namelessmc.bot;

@Deprecated
public class Queries {

//    public static boolean newGuild(String guild_id, String owner_id) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("INSERT INTO guilds (`guild_id`, `owner_id`) VALUES (?, ?)");
//            preparedStatement.setString(1, guild_id);
//            preparedStatement.setString(2, owner_id);
//            preparedStatement.executeUpdate();
//            return true;
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return false;
//        }
//    }
//
//    public static boolean setGuildApiUrl(String guild_id, String api_url) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("UPDATE guilds SET `api_url` = ? WHERE `guild_id` = ?");
//            preparedStatement.setString(1, api_url);
//            preparedStatement.setString(2, guild_id);
//            return preparedStatement.executeUpdate() == 1;
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return false;
//        }
//    }
//
//    public static String getGuildApiUrl(String guild_id) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("SELECT `api_url` FROM guilds WHERE `guild_id` = ?");
//            preparedStatement.setString(1, guild_id);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (!resultSet.next()) return null;
//            else return resultSet.getString("api_url");
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return null;
//        }
//    }
//
//    public static List<String> getUserGuilds(String owner_id) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("SELECT `guild_id` FROM guilds WHERE `owner_id` = ?");
//            preparedStatement.setString(1, owner_id);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (!resultSet.next()) return null;
//            else {
//                List<String> guild_ids = new ArrayList<>();
//                guild_ids.add(resultSet.getString("guild_id").trim());
//                while (resultSet.next()) {
//                    guild_ids.add(resultSet.getString("guild_id").trim());
//                }
//                return guild_ids;
//            }
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return null;
//        }
//    }
//
//    public static boolean addPendingVerification(String discord_id, String token, String guild_id, String role, String site) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("INSERT INTO pending_verifications (`discord_id`, `token`, `guild_id`, `role`, `site`) VALUES (?, ?, ?, ?, ?)");
//            preparedStatement.setString(1, discord_id);
//            preparedStatement.setString(2, token);
//            preparedStatement.setString(3, guild_id);
//            preparedStatement.setString(4, role);
//            preparedStatement.setString(5, site);
//            preparedStatement.executeUpdate();
//            return true;
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return false;
//        }
//    }
//
//    public static PendingVerification getPendingVerification(String discord_id) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("SELECT `token`, `guild_id`, `role`, `site` FROM pending_verifications WHERE `discord_id` = ?");
//            preparedStatement.setString(1, discord_id);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (!resultSet.next()) return null;
//            else return new PendingVerification(discord_id, resultSet.getString("token"), resultSet.getString("guild_id"), resultSet.getString("role"), resultSet.getString("site"));
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return null;
//        }
//    }
//
//    public static boolean removePendingVerification(String discord_id) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("DELETE FROM `pending_verifications` WHERE `discord_id` = ?");
//            preparedStatement.setString(1, discord_id);
//            return preparedStatement.executeUpdate() > 0;
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return false;
//        }
//    }
//
//    public static HashMap<String, Language> userLanguageCache = new HashMap<>();
//
//    public static boolean setUserLanguage(String user_id, String language) {
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("INSERT INTO user_languages (`discord_id`, `language`) VALUES (?, ?) ON DUPLICATE KEY UPDATE `language` = ?");
//            preparedStatement.setString(1, user_id);
//            preparedStatement.setString(2, language);
//            preparedStatement.setString(3, language);
//            preparedStatement.executeUpdate();
//            userLanguageCache.put(user_id, new Language(language));
//            return true;
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return false;
//        }
//    }
//
//    public static Language getUserLanguage(String user_id) {
//        if (userLanguageCache.containsKey(user_id)) return userLanguageCache.get(user_id);
//        try {
//            PreparedStatement preparedStatement = Main.getConnection().prepareStatement("SELECT `language` FROM user_languages WHERE `discord_id` = ?");
//            preparedStatement.setString(1, user_id);
//            ResultSet resultSet = preparedStatement.executeQuery();
//            if (!resultSet.next()) {
//                setUserLanguage(user_id, "EnglishUK");
//                return new Language("EnglishUK");
//            }
//            else {
//                Language language = new Language(resultSet.getString("language"));
//                userLanguageCache.put(user_id, language);
//                return language;
//            }
//        } catch (SQLException exception) {
//            exception.printStackTrace();
//            return null;
//        }
//    }
	
}
