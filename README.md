# Nameless-Link

The "Nameless Link" Discord bot synchronizes user roles to and from a specific Discord Guild.

### Setup:

**Note: Requires [NamelessMC](https://github.com/NamelessMC/Nameless/tree/v2) pre-8.**

**Note: Self-hosting is available, but not recommended for beginners. Check the wiki for a self-hosting guide.**

1. Invite the bot to your guild & Discord setup
    * **Note: You are required to have the "Manage Roles" permission in the guild in order to invite the bot.**
    * The owner of the guild will be PM'd by the bot, asking for the API URL of their NamelessMC website. This can be found in `StaffCP - Configuration - API`. The owner of the guild must reply with the API URL. If the URL is valid, they will receive a success message - nicely done!
 
2. Enable Discord Integration in NamelessMC
    * Open `StaffCP - Integrations - Discord`, and click the switch. 
    * Enter your [Discord Guild ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) (Must be same guild that the bot joined in step 1) and then hit Submit.
    * **Note: Only edit the Bot URL if you are self-hosting. No Discord integrations will work if this setting is invalid. You have been warned!**
 
3. Configure NamelessMC Groups
    * In order for the bot to know which Discord role to assign to each NamelessMC group, you must setup the [Discord Role ID](https://discordhelp.net/role-id) for any groups you want to sync.
    * Any groups without a Role ID will simply be ignored.
    * **Note: When a role is removed from a user on Discord, and they have linked their accounts (see step 4), their NamelessMC group will be set to the default post-validation group.**

4. Link NamelessMC and Discord accounts
    * In order for Discord to know which user is associated with each guild member, people who wish to have their ranks synced must link their accounts using the following steps.
    * Open `Account - Profile Settings`, and paste your [Discord User ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) in the box.
    * If the ID is valid, you will receive a PM from the bot. To verify that you own the account, you must reply to the bot with the username you used to register on the website.
    * Once you reply with the correct username, you will see a success message from the bot. Well done!
    * **Note: For security reasons, it is impossible for anyone who does not have access to the database directly to edit a user's Discord ID. Their ID will be set once they verify they own it.**

### Contributors:
* @Aberdeener (Main codebase)
* @yangyang200 (Chinese-Simplified language)
* @Fjuro (Czech language)
* @IsS127 (Swedish language)
