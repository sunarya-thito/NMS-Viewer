package org.bukkit.craftbukkit.util;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.server.ChatClickable;
import net.minecraft.server.ChatClickable.EnumClickAction;
import net.minecraft.server.ChatComponentText;
import net.minecraft.server.ChatMessage;
import net.minecraft.server.ChatModifier;
import net.minecraft.server.EnumChatFormat;
import net.minecraft.server.IChatBaseComponent;
import org.bukkit.ChatColor;

public final class CraftChatMessage {

    private static final Pattern LINK_PATTERN = Pattern.compile("((?:(?:https?):\\/\\/)?(?:[-\\w_\\.]{2,}\\.[a-z]{2,4}.*?(?=[\\.\\?!,;:]?(?:[" + String.valueOf(org.bukkit.ChatColor.COLOR_CHAR) + " \\n]|$))))");
    private static final Map<Character, EnumChatFormat> formatMap;

    static {
        Builder<Character, EnumChatFormat> builder = ImmutableMap.builder();
        for (EnumChatFormat format : EnumChatFormat.values()) {
            builder.put(Character.toLowerCase(format.toString().charAt(1)), format);
        }
        formatMap = builder.build();
    }

    public static EnumChatFormat getColor(ChatColor color) {
        return formatMap.get(color.getChar());
    }

    public static ChatColor getColor(EnumChatFormat format) {
        return ChatColor.getByChar(format.character);
    }

    private static final class StringMessage {
        private static final Pattern INCREMENTAL_PATTERN = Pattern.compile("(" + String.valueOf(org.bukkit.ChatColor.COLOR_CHAR) + "[0-9a-fk-or])|((?:(?:https?):\\/\\/)?(?:[-\\w_\\.]{2,}\\.[a-z]{2,4}.*?(?=[\\.\\?!,;:]?(?:[" + String.valueOf(org.bukkit.ChatColor.COLOR_CHAR) + " \\n]|$))))|(\\n)", Pattern.CASE_INSENSITIVE);
        // Separate pattern with no group 3, new lines are part of previous string
        private static final Pattern INCREMENTAL_PATTERN_KEEP_NEWLINES = Pattern.compile("(" + String.valueOf(org.bukkit.ChatColor.COLOR_CHAR) + "[0-9a-fk-or])|((?:(?:https?):\\/\\/)?(?:[-\\w_\\.]{2,}\\.[a-z]{2,4}.*?(?=[\\.\\?!,;:]?(?:[" + String.valueOf(org.bukkit.ChatColor.COLOR_CHAR) + " ]|$))))", Pattern.CASE_INSENSITIVE);

        private final List<IChatBaseComponent> list = new ArrayList<IChatBaseComponent>();
        private IChatBaseComponent currentChatComponent = new ChatComponentText("");
        private ChatModifier modifier = new ChatModifier();
        private final IChatBaseComponent[] output;
        private int currentIndex;
        private final String message;

        private StringMessage(String message, boolean keepNewlines) {
            this.message = message;
            if (message == null) {
                output = new IChatBaseComponent[]{currentChatComponent};
                return;
            }
            list.add(currentChatComponent);

            Matcher matcher = (keepNewlines ? INCREMENTAL_PATTERN_KEEP_NEWLINES : INCREMENTAL_PATTERN).matcher(message);
            String match = null;
            boolean needsAdd = false;
            while (matcher.find()) {
                int groupId = 0;
                while ((match = matcher.group(++groupId)) == null) {
                    // NOOP
                }
                int index = matcher.start(groupId);
                if (index > currentIndex) {
                    needsAdd = false;
                    appendNewComponent(index);
                }
                switch (groupId) {
                case 1:
                    EnumChatFormat format = formatMap.get(match.toLowerCase(java.util.Locale.ENGLISH).charAt(1));
                    if (format.isFormat() && format != EnumChatFormat.RESET) {
                        switch (format) {
                        case BOLD:
                            modifier.setBold(Boolean.TRUE);
                            break;
                        case ITALIC:
                            modifier.setItalic(Boolean.TRUE);
                            break;
                        case STRIKETHROUGH:
                            modifier.setStrikethrough(Boolean.TRUE);
                            break;
                        case UNDERLINE:
                            modifier.setUnderline(Boolean.TRUE);
                            break;
                        case OBFUSCATED:
                            modifier.setRandom(Boolean.TRUE);
                            break;
                        default:
                            throw new AssertionError("Unexpected message format");
                        }
                    } else { // Color resets formatting
                        modifier = new ChatModifier().setColor(format);
                    }
                    needsAdd = true;
                    break;
                case 2:
                    if (!(match.startsWith("http://") || match.startsWith("https://"))) {
                        match = "http://" + match;
                    }
                    modifier.setChatClickable(new ChatClickable(EnumClickAction.OPEN_URL, match));
                    appendNewComponent(matcher.end(groupId));
                    modifier.setChatClickable((ChatClickable) null);
                    break;
                case 3:
                    if (needsAdd) {
                        appendNewComponent(index);
                    }
                    currentChatComponent = null;
                    break;
                }
                currentIndex = matcher.end(groupId);
            }

            if (currentIndex < message.length() || needsAdd) {
                appendNewComponent(message.length());
            }

            output = list.toArray(new IChatBaseComponent[list.size()]);
        }

        private void appendNewComponent(int index) {
            IChatBaseComponent addition = new ChatComponentText(message.substring(currentIndex, index)).setChatModifier(modifier);
            currentIndex = index;
            modifier = modifier.clone();
            if (modifier.getColor() == EnumChatFormat.RESET) {
                modifier.setColor(null);
            }
            if (currentChatComponent == null) {
                currentChatComponent = new ChatComponentText("");
                list.add(currentChatComponent);
            }
            currentChatComponent.addSibling(addition);
        }

        private IChatBaseComponent[] getOutput() {
            return output;
        }
    }

    public static IChatBaseComponent wrapOrNull(String message) {
        return (message == null || message.isEmpty()) ? null : new ChatComponentText(message);
    }

    public static IChatBaseComponent wrapOrEmpty(String message) {
        return (message == null) ? new ChatComponentText("") : new ChatComponentText(message);
    }

    public static IChatBaseComponent fromStringOrNull(String message) {
        return fromStringOrNull(message, false);
    }

    public static IChatBaseComponent fromStringOrNull(String message, boolean keepNewlines) {
        return (message == null || message.isEmpty()) ? null : fromString(message, keepNewlines)[0];
    }

    public static IChatBaseComponent[] fromString(String message) {
        return fromString(message, false);
    }

    public static IChatBaseComponent[] fromString(String message, boolean keepNewlines) {
        return new StringMessage(message, keepNewlines).getOutput();
    }

    public static String toJSON(IChatBaseComponent component) {
        return IChatBaseComponent.ChatSerializer.a(component);
    }

    public static String fromComponent(IChatBaseComponent component) {
        if (component == null) return "";
        StringBuilder out = new StringBuilder();

        boolean hadFormat = false;
        for (IChatBaseComponent c : component) {
            ChatModifier modi = c.getChatModifier();
            EnumChatFormat color = modi.getColor();
            if (!c.getText().isEmpty() || color != null) {
                if (color != null) {
                    out.append(color);
                    if (color != EnumChatFormat.RESET) {
                        hadFormat = true;
                    }
                } else if (hadFormat) {
                    out.append(ChatColor.RESET);
                    hadFormat = false;
                }
            }
            if (modi.isBold()) {
                out.append(EnumChatFormat.BOLD);
            }
            if (modi.isItalic()) {
                out.append(EnumChatFormat.ITALIC);
            }
            if (modi.isUnderlined()) {
                out.append(EnumChatFormat.UNDERLINE);
            }
            if (modi.isStrikethrough()) {
                out.append(EnumChatFormat.STRIKETHROUGH);
            }
            if (modi.isRandom()) {
                out.append(EnumChatFormat.OBFUSCATED);
            }
            out.append(c.getText());
        }
        return out.toString();
    }

    public static IChatBaseComponent fixComponent(IChatBaseComponent component) {
        Matcher matcher = LINK_PATTERN.matcher("");
        return fixComponent(component, matcher);
    }

    private static IChatBaseComponent fixComponent(IChatBaseComponent component, Matcher matcher) {
        if (component instanceof ChatComponentText) {
            ChatComponentText text = ((ChatComponentText) component);
            String msg = text.getText();
            if (matcher.reset(msg).find()) {
                matcher.reset();

                ChatModifier modifier = text.getChatModifier() != null ? text.getChatModifier() : new ChatModifier();
                List<IChatBaseComponent> extras = new ArrayList<IChatBaseComponent>();
                List<IChatBaseComponent> extrasOld = new ArrayList<IChatBaseComponent>(text.getSiblings());
                component = text = new ChatComponentText("");

                int pos = 0;
                while (matcher.find()) {
                    String match = matcher.group();

                    if (!(match.startsWith("http://") || match.startsWith("https://"))) {
                        match = "http://" + match;
                    }

                    ChatComponentText prev = new ChatComponentText(msg.substring(pos, matcher.start()));
                    prev.setChatModifier(modifier);
                    extras.add(prev);

                    ChatComponentText link = new ChatComponentText(matcher.group());
                    ChatModifier linkModi = modifier.clone();
                    linkModi.setChatClickable(new ChatClickable(EnumClickAction.OPEN_URL, match));
                    link.setChatModifier(linkModi);
                    extras.add(link);

                    pos = matcher.end();
                }

                ChatComponentText prev = new ChatComponentText(msg.substring(pos));
                prev.setChatModifier(modifier);
                extras.add(prev);
                extras.addAll(extrasOld);

                for (IChatBaseComponent c : extras) {
                    text.addSibling(c);
                }
            }
        }

        List<IChatBaseComponent> extras = component.getSiblings();
        for (int i = 0; i < extras.size(); i++) {
            IChatBaseComponent comp = extras.get(i);
            if (comp.getChatModifier() != null && comp.getChatModifier().getClickEvent() == null) {
                extras.set(i, fixComponent(comp, matcher));
            }
        }

        if (component instanceof ChatMessage) {
            Object[] subs = ((ChatMessage) component).getArgs();
            for (int i = 0; i < subs.length; i++) {
                Object comp = subs[i];
                if (comp instanceof IChatBaseComponent) {
                    IChatBaseComponent c = (IChatBaseComponent) comp;
                    if (c.getChatModifier() != null && c.getChatModifier().getClickEvent() == null) {
                        subs[i] = fixComponent(c, matcher);
                    }
                } else if (comp instanceof String && matcher.reset((String) comp).find()) {
                    subs[i] = fixComponent(new ChatComponentText((String) comp), matcher);
                }
            }
        }

        return component;
    }

    private CraftChatMessage() {
    }
}
