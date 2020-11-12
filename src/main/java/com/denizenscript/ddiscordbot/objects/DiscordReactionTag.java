package com.denizenscript.ddiscordbot.objects;

import com.denizenscript.ddiscordbot.DenizenDiscordBot;
import com.denizenscript.ddiscordbot.DiscordConnection;
import com.denizenscript.denizen.utilities.debugging.Debug;
import com.denizenscript.denizencore.objects.ArgumentHelper;
import com.denizenscript.denizencore.objects.Fetchable;
import com.denizenscript.denizencore.objects.ObjectTag;
import com.denizenscript.denizencore.objects.core.ElementTag;
import com.denizenscript.denizencore.objects.core.ListTag;
import com.denizenscript.denizencore.tags.Attribute;
import com.denizenscript.denizencore.tags.ObjectTagProcessor;
import com.denizenscript.denizencore.tags.TagContext;
import com.denizenscript.denizencore.tags.TagRunnable;
import com.denizenscript.denizencore.utilities.CoreUtilities;
import net.dv8tion.jda.api.entities.*;

import java.util.List;

public class DiscordReactionTag implements ObjectTag {

    // <--[language]
    // @name DiscordReactionTag Objects
    // @group Object System
    // @plugin dDiscordBot
    // @description
    // A DiscordReactionTag is an object that represents a reaction to a message already sent on Discord, as a generic reference.
    //
    // These use the object notation "discordreaction@".
    // The identity format for Discord reactions is the bot ID, followed by the channel ID, followed by the message ID, followed by the reaction ID.
    // Or: mybot,12,1234,99
    // The reaction ID for custom reactions is an ID number, and for default emojis is the unicode text format of the emoji.
    //
    // -->

    @Fetchable("discordreaction")
    public static DiscordReactionTag valueOf(String string, TagContext context) {
        if (string.startsWith("discordreaction@")) {
            string = string.substring("discordreaction@".length());
        }
        if (string.contains("@")) {
            return null;
        }
        List<String> commaSplit = CoreUtilities.split(string, ',');
        if (commaSplit.size() != 4) {
            if (context == null || context.showErrors()) {
                Debug.echoError("DiscordReactionTag input is not valid.");
            }
            return null;
        }
        String msgIdText = commaSplit.get(2);
        if (!ArgumentHelper.matchesInteger(msgIdText)) {
            if (context == null || context.showErrors()) {
                Debug.echoError("DiscordReactionTag message input is not a number.");
            }
            return null;
        }
        long msgId = Long.parseLong(msgIdText);
        if (msgId == 0) {
            return null;
        }
        String chanIdText = commaSplit.get(1);
        if (!ArgumentHelper.matchesInteger(chanIdText)) {
            if (context == null || context.showErrors()) {
                Debug.echoError("DiscordReactionTag channel ID input is not a number.");
            }
            return null;
        }
        long chanId = Long.parseLong(chanIdText);
        if (chanId == 0) {
            return null;
        }
        return new DiscordReactionTag(commaSplit.get(0), chanId, msgId, commaSplit.get(3));
    }

    public static boolean matches(String arg) {
        if (arg.startsWith("discordreaction@")) {
            return true;
        }
        if (arg.contains("@")) {
            return false;
        }
        if (CoreUtilities.split(arg, ',').size() != 4) {
            return false;
        }
        return ArgumentHelper.matchesInteger(arg.substring(arg.lastIndexOf(',') + 1));
    }

    public DiscordReactionTag(String bot, long channel_id, long message_id, String reaction) {
        this.bot = bot;
        this.channel_id = channel_id;
        this.message_id = message_id;
        if (ArgumentHelper.matchesInteger(reaction)) {
            this.emote = MessageReaction.ReactionEmote.fromCustom(getBot().client.getEmoteById(Long.parseLong(reaction)));
        }
        else {
            this.emote = MessageReaction.ReactionEmote.fromUnicode(reaction, getBot().client);
        }
    }

    public DiscordReactionTag(String bot, long channelId, long messageId, MessageReaction reaction) {
        this.bot = bot;
        this.emote = reaction.getReactionEmote();
        this.message_id = messageId;
        this.channel_id = channelId;
        this.reaction = reaction;
    }

    public DiscordReactionTag(String bot, Message message, MessageReaction reaction) {
        this.bot = bot;
        this.emote = reaction.getReactionEmote();
        this.message_id = message.getIdLong();
        this.message = message;
        this.channel = message.getTextChannel();
        this.channel_id = message.getTextChannel().getIdLong();
        this.reaction = reaction;
    }

    public DiscordConnection getBot() {
        return DenizenDiscordBot.instance.connections.get(bot);
    }

    public TextChannel getChannel() {
        if (channel != null) {
            return channel;
        }
        channel = getBot().client.getTextChannelById(channel_id);
        return channel;
    }

    public Message getMessage() {
        if (message != null) {
            return message;
        }
        message = getChannel().retrieveMessageById(message_id).complete();
        return message;
    }

    public MessageReaction getReaction() {
        if (reaction != null) {
            return reaction;
        }
        for (MessageReaction reaction : getMessage().getReactions()) {
            if (reaction.getReactionEmote().equals(emote)) {
                this.reaction = reaction;
                return reaction;
            }
        }
        return null;
    }

    public String bot;

    public TextChannel channel;

    public Message message;

    public MessageReaction.ReactionEmote emote;

    public long channel_id;

    public long message_id;

    public MessageReaction reaction;

    public static void registerTags() {

        // <--[tag]
        // @attribute <DiscordReactionTag.id>
        // @returns ElementTag
        // @plugin dDiscordBot
        // @description
        // Returns the ID of the reaction emote.
        // For custom emoji, this is a numeric ID. For default emoji, this is the unicode symbol.
        // -->
        registerTag("id", (attribute, object) -> {
            return new ElementTag(object.emote.isEmoji() ? object.emote.getEmoji() : object.emote.getEmote().getId());
        });

        // <--[tag]
        // @attribute <DiscordReactionTag.message>
        // @returns DiscordMessageTag
        // @plugin dDiscordBot
        // @description
        // Returns the message this reaction is attached to.
        // -->
        registerTag("message", (attribute, object) -> {
            if (object.message == null) {
                return new DiscordMessageTag(object.bot, object.channel_id, object.message_id);
            }
            else {
                return new DiscordMessageTag(object.bot, object.message);
            }
        });

        // <--[tag]
        // @attribute <DiscordReactionTag.count>
        // @returns ElementTag(Number)
        // @plugin dDiscordBot
        // @description
        // Returns the amount of times this reaction exists on the message.
        // -->
        registerTag("count", (attribute, object) -> {
            if (object.getReaction().hasCount()) {
                return new ElementTag(object.getReaction().getCount());
            }
            return new ElementTag(object.getReaction().retrieveUsers().complete().size());
        });

        // <--[tag]
        // @attribute <DiscordReactionTag.name>
        // @returns ElementTag
        // @plugin dDiscordBot
        // @description
        // Returns the amount of the emoji reacted.
        // -->
        registerTag("name", (attribute, object) -> {
            return new ElementTag(object.emote.getName());
        });

        // <--[tag]
        // @attribute <DiscordReactionTag.is_animated>
        // @returns ElementTag
        // @plugin dDiscordBot
        // @description
        // Returns whether the emote reacted is animated (an "animoji").
        // -->
        registerTag("is_animated", (attribute, object) -> {
            return new ElementTag(object.emote.isEmote() && object.emote.getEmote().isAnimated());
        });

        // <--[tag]
        // @attribute <DiscordReactionTag.reactors>
        // @returns ListTag(DiscordUserTag)
        // @plugin dDiscordBot
        // @description
        // Returns the list of users that added this reaction to the message.
        // -->
        registerTag("reactors", (attribute, object) -> {
            ListTag users = new ListTag();
            for (User user : object.getReaction().retrieveUsers().complete()) {
                users.addObject(new DiscordUserTag(object.bot, user));
            }
            return users;
        });
    }

    public static ObjectTagProcessor<DiscordReactionTag> tagProcessor = new ObjectTagProcessor<>();

    public static void registerTag(String name, TagRunnable.ObjectInterface<DiscordReactionTag> runnable, String... variants) {
        tagProcessor.registerTag(name, runnable, variants);
    }

    @Override
    public ObjectTag getObjectAttribute(Attribute attribute) {
        return tagProcessor.getObjectAttribute(this, attribute);
    }

    String prefix = "discordreaction";

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
        return true;
    }

    @Override
    public String getObjectType() {
        return "DiscordReaction";
    }

    @Override
    public String identify() {
        return "discordreaction@" + bot + "," + channel_id + "," + message_id + "," + (emote.isEmoji() ? emote.getEmoji() : emote.getEmote().getId());
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
