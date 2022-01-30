package net.minecraft.network.chat;

import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.EnumChatFormat;

public final class ChatHexColor {

    private static final String CUSTOM_COLOR_PREFIX = "#";
    private static final Map<EnumChatFormat, ChatHexColor> LEGACY_FORMAT_TO_COLOR = (Map) Stream.of(EnumChatFormat.values()).filter(EnumChatFormat::isColor).collect(ImmutableMap.toImmutableMap(Function.identity(), (enumchatformat) -> {
        return new ChatHexColor(enumchatformat.getColor(), enumchatformat.getName(), enumchatformat); // CraftBukkit
    }));
    private static final Map<String, ChatHexColor> NAMED_COLORS = (Map) ChatHexColor.LEGACY_FORMAT_TO_COLOR.values().stream().collect(ImmutableMap.toImmutableMap((chathexcolor) -> {
        return chathexcolor.name;
    }, Function.identity()));
    private final int value;
    @Nullable
    public final String name;
    // CraftBukkit start
    @Nullable
    public final EnumChatFormat format;

    private ChatHexColor(int i, String s, EnumChatFormat format) {
        this.value = i;
        this.name = s;
        this.format = format;
    }

    private ChatHexColor(int i) {
        this.value = i;
        this.name = null;
        this.format = null;
    }
    // CraftBukkit end

    public int getValue() {
        return this.value;
    }

    public String serialize() {
        return this.name != null ? this.name : this.formatValue();
    }

    private String formatValue() {
        return String.format("#%06X", this.value);
    }

    public boolean equals(Object object) {
        if (this == object) {
            return true;
        } else if (object != null && this.getClass() == object.getClass()) {
            ChatHexColor chathexcolor = (ChatHexColor) object;

            return this.value == chathexcolor.value;
        } else {
            return false;
        }
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.value, this.name});
    }

    public String toString() {
        return this.name != null ? this.name : this.formatValue();
    }

    @Nullable
    public static ChatHexColor fromLegacyFormat(EnumChatFormat enumchatformat) {
        return (ChatHexColor) ChatHexColor.LEGACY_FORMAT_TO_COLOR.get(enumchatformat);
    }

    public static ChatHexColor fromRgb(int i) {
        return new ChatHexColor(i);
    }

    @Nullable
    public static ChatHexColor parseColor(String s) {
        if (s.startsWith("#")) {
            try {
                int i = Integer.parseInt(s.substring(1), 16);

                return fromRgb(i);
            } catch (NumberFormatException numberformatexception) {
                return null;
            }
        } else {
            return (ChatHexColor) ChatHexColor.NAMED_COLORS.get(s);
        }
    }
}
