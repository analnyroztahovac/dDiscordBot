package com.denizenscript.ddiscordbot.objects;

import com.denizenscript.ddiscordbot.DiscordConnection;
import com.denizenscript.ddiscordbot.DenizenDiscordBot;
import com.denizenscript.denizencore.objects.*;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagRunnable;
import discord4j.core.object.entity.*;
import discord4j.core.object.util.Snowflake;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.utilities.CoreUtilities;

public class DiscordGroupTag implements ObjectTag {

    // <--[language]
    // @name DiscordGroupTag
    // @group Object System
    // @plugin dDiscordBot
    // @description
    // A DiscordGroupTag is an object that represents a group on Discord, either as a generic reference,
    // or as a bot-specific reference.
    //
    // Note that the correct name for what we call here a 'group' is inconsistent between different people.
    // The Discord API calls it a "guild" (for historical reasons, not called that by *people* anymore usually),
    // messages in the Discord app call it a "server" (which is a convenient name but is factually inaccurate, as they are not servers),
    // many people will simply say "a Discord" (which is awkward for branding and also would be confusing if used in documentation).
    // So we're going with "group" (which is still confusing because "group" sometimes refers to DM groups, but... it's good enough).
    //
    // For format info, see <@link language discordgroup@>
    //
    // -->

    // <--[language]
    // @name discordgroup@
    // @group Object Fetcher System
    // @plugin dDiscordBot
    // @description
    // discordgroup@ refers to the 'object identifier' of a DiscordGroupTag. The 'discordgroup@' is notation for Denizen's Object
    // Fetcher. The constructor for a DiscordGroupTag is the bot ID (optional), followed by the guild ID (required).
    // For example: 1234
    // Or: mybot,1234
    //
    // For general info, see <@link language DiscordGroupTag>
    // -->

    @Fetchable("discordgroup")
    public static DiscordGroupTag valueOf(String string, TagContext context) {
        if (string.startsWith("discordgroup@")) {
            string = string.substring("discordgroup@".length());
        }
        if (string.contains("@")) {
            return null;
        }
        int comma = string.indexOf(',');
        String bot = null;
        if (comma > 0) {
            bot = CoreUtilities.toLowerCase(string.substring(0, comma));
            string = string.substring(comma + 1);
        }
        long grpId = ArgumentHelper.getLongFrom(string);
        if (grpId == 0) {
            return null;
        }
        return new DiscordGroupTag(bot, grpId);
    }

    public static boolean matches(String arg) {
        if (arg.startsWith("discordgroup@")) {
            return true;
        }
        if (arg.contains("@")) {
            return false;
        }
        int comma = arg.indexOf(',');
        if (comma == -1) {
            return ArgumentHelper.matchesInteger(arg);
        }
        if (comma == arg.length() - 1) {
            return false;
        }
        return ArgumentHelper.matchesInteger(arg.substring(comma + 1));
    }

    public DiscordGroupTag(String bot, long guildId) {
        this.bot = bot;
        this.guild_id = guildId;
        if (bot != null) {
            DiscordConnection conn = DenizenDiscordBot.instance.connections.get(bot);
            if (conn != null) {
                guild = conn.client.getGuildById(Snowflake.of(guild_id)).block();
            }
        }
    }

    public DiscordGroupTag(String bot, Guild guild) {
        this.bot = bot;
        this.guild = guild;
        guild_id = guild.getId().asLong();
    }

    public Guild guild;

    public String bot;

    public long guild_id;

    public static void registerTags() {

        // <--[tag]
        // @attribute <DiscordGroupTag.name>
        // @returns ElementTag
        // @plugin dDiscordBot
        // @description
        // Returns the name of the group.
        // -->
        registerTag("name", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                return new ElementTag(object.guild.getName());
            }
        });

        // <--[tag]
        // @attribute <DiscordGroupTag.id>
        // @returns ElementTag(Number)
        // @plugin dDiscordBot
        // @description
        // Returns the ID number of the group.
        // -->
        registerTag("id", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                return new ElementTag(object.guild_id);
            }
        });

        // <--[tag]
        // @attribute <DiscordGroupTag.channels>
        // @returns ListTag(DiscordChannelTag)
        // @plugin dDiscordBot
        // @description
        // Returns a list of all channels in the group.
        // -->
        registerTag("channels", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                ListTag list = new ListTag();
                for (GuildChannel chan : object.guild.getChannels().toIterable()) {
                    list.addObject(new DiscordChannelTag(object.bot, chan));
                }
                return list;
            }
        });

        // <--[tag]
        // @attribute <DiscordGroupTag.members>
        // @returns ListTag(DiscordUserTag)
        // @plugin dDiscordBot
        // @description
        // Returns a list of all users in the group.
        // -->
        registerTag("members", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                ListTag list = new ListTag();
                for (Member member : object.guild.getMembers().toIterable()) {
                    list.addObject(new DiscordUserTag(object.bot, member.getId().asLong()));
                }
                return list;
            }
        });

        // <--[tag]
        // @attribute <DiscordGroupTag.roles>
        // @returns ListTag(DiscordRoleTag)
        // @plugin dDiscordBot
        // @description
        // Returns a list of all roles in the group.
        // -->
        registerTag("roles", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                ListTag list = new ListTag();
                for (Role role : object.guild.getRoles().toIterable()) {
                    list.addObject(new DiscordRoleTag(object.bot, role));
                }
                return list;
            }
        });

        // <--[tag]
        // @attribute <DiscordGroupTag.member[<name>]>
        // @returns DiscordUserTag
        // @plugin dDiscordBot
        // @description
        // Returns the group member that best matches the input name, or null if there's no match.
        // For input of username#id, will always only match for the exact user.
        // For input of only the username, return value might be unexpected if multiple members have the same username
        // (this happens more often than you might expect - many users accidentally join new Discord groups from the
        // web on a temporary web account, then rejoin on a local client with their 'real' account).
        // -->
        registerTag("member", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                String matchString = CoreUtilities.toLowerCase(attribute.getContext(1));
                int discrimMark = matchString.indexOf('#');
                String discrimVal = null;
                if (discrimMark > 0 && discrimMark == matchString.length() - 5) {
                    discrimVal = matchString.substring(discrimMark + 1);
                    matchString = matchString.substring(0, discrimMark);
                }
                final String discrim = discrimVal;
                final String matchName = matchString;
                for (Member member : object.guild.getMembers().filter(
                        m -> matchName.equalsIgnoreCase(m.getUsername()) && (discrim == null || discrim.equals(m.getDiscriminator()))).toIterable()) {
                    return new DiscordUserTag(object.bot, member.getId().asLong());
                }
                return null;
            }
        });

        // <--[tag]
        // @attribute <DiscordGroupTag.channel[<name>]>
        // @returns DiscordChannelTag
        // @plugin dDiscordBot
        // @description
        // Returns the channel that best matches the input name, or null if there's no match.
        // -->
        registerTag("channel", new TagRunnable.ObjectForm<DiscordGroupTag>() {
            @Override
            public ObjectTag run(Attribute attribute, DiscordGroupTag object) {
                if (!attribute.hasContext(1)) {
                    return null;
                }
                String matchString = CoreUtilities.toLowerCase(attribute.getContext(1));
                Channel bestMatch = null;
                for (GuildChannel chan : object.guild.getChannels().toIterable()) {
                    String chanName = CoreUtilities.toLowerCase(chan.getName());
                    if (matchString.equals(chanName)) {
                        bestMatch = chan;
                        break;
                    }
                    if (chanName.contains(matchString)) {
                        bestMatch = chan;
                    }
                }
                if (bestMatch == null) {
                    return null;
                }
                return new DiscordChannelTag(object.bot, bestMatch);
            }
        });
    }

    public static ObjectTagProcessor<DiscordGroupTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectForm<DiscordGroupTag> runnable) {
        tagProcessor.registerTag(name, runnable);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    String prefix = "discordgroup";

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public String debug() {
        return (prefix + "='<A>" + identify() + "<G>'  ");
    }

    @Override
    public boolean isUnique() {
        return false;
    }

    @Override
    public String getObjectType() {
        return "DiscordGroup";
    }

    @Override
    public String identify() {
        if (bot != null) {
            return "discordgroup@" + bot + "," + guild_id;
        }
        return "discordgroup@" + guild_id;
    }

    @Override
    public String identifySimple() {
        return identify();
    }

    @Override
    public String toString() {
        return identify();
    }

    @Override
    public ObjectTag setPrefix(String prefix) {
        if (prefix != null) {
            this.prefix = prefix;
        }
        return this;
    }
}