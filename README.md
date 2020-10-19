# Nameless-Link

The "Nameless Link" Discord bot synchronizes user roles to and from a specific Discord Guild.

### Setup:

*Note: Requires [NamelessMC](https://github.com/NamelessMC/Nameless/tree/v2) v2 pre-8.*

#### [Setup Video](https://www.youtube.com/watch?v=oc_e6GFjIVo) if you prefer.

*Note: Self-hosting is available, but not recommended for beginners. Check the wiki for a self-hosting guide.*

1. Invite the bot to your guild & Discord setup
    * *Note: You are required to have the "Manage Roles" permission in the guild in order to invite the bot. As well, the bot needs to always have its self-assigned role, and the role must be able to interact with all other users in the Guild*
    * [Invite the bot using this link](https://discord.com/api/oauth2/authorize?client_id=734609142081388705&permissions=268435456&scope=bot).
    * The owner of the guild will be PM'd by the bot, asking for the API URL of their NamelessMC website. This can be found in `StaffCP - Configuration - API`. The owner of the guild must reply with `!url ` followed by their API URL. If the URL is valid, they will receive a success message - nicely done!
 
2. Enable Discord Integration in NamelessMC
    * Open `StaffCP - Integrations - Discord`, and click the switch. 
    * Enter your [Discord Guild ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) (Must be same guild that the bot joined in step 1) and then hit Submit.
    * *Note: Only edit the Bot URL if you are self-hosting. No Nameless Link integrations will work if this setting is invalid. You have been warned!*
 
3. Configure NamelessMC Groups
    * In order for the bot to know which Discord role to assign to each NamelessMC group, you must setup the [Discord Role ID](https://discordhelp.net/role-id) for any groups you want to sync. To do this, open `StaffCP - Configuration - API - Group Sync` and paste the Role ID in the applicable row.
    * Any groups without a Role ID will simply be ignored.
    * *Note: When a role is removed from a user on Discord, and they have linked their accounts (see step 4), their NamelessMC group will be set to the default post-validation group.*

4. Link NamelessMC and Discord accounts
    * In order for Discord to know which user is associated with each guild member, people who wish to have their ranks synced must link their accounts using the following steps.
    * Open `Account - Profile Settings`, and paste your [Discord User ID](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-) in the box.
    * If the ID is valid, you will receive a PM from the bot. To verify that you own the account, you must reply to the bot with the username you used to register on the website.
    * Once you reply with the correct username, you will see a success message from the bot. Well done!
    * *Note: For security reasons, it is impossible for anyone who does not have access to the database directly to edit a user's Discord ID. Their ID will be set once they verify they own it.*

### Contributors:
* @Aberdeener (Codebase)
* @Derkades (Codebade & Dutch language)
* @yangyang200 (Chinese-Simplified language)
* @Fjuro (Czech language)
* @IsS127 (Swedish language) [In Progress]
* @Govindass (Lithuanian language)
* @Hilligans (French language) [In Progress]
* @nUKEmAN4 (Croatian language)
* @kacperleague9 (Polish language)
* @zJerino (Spanish language)
* @MissChikoo (Norwegian/Bokmål language) [In Progress]
