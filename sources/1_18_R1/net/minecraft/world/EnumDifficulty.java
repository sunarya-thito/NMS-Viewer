package net.minecraft.world;

import java.util.Arrays;
import java.util.Comparator;
import javax.annotation.Nullable;
import net.minecraft.network.chat.ChatMessage;
import net.minecraft.network.chat.IChatBaseComponent;

public enum EnumDifficulty {

    PEACEFUL(0, "peaceful"), EASY(1, "easy"), NORMAL(2, "normal"), HARD(3, "hard");

    private static final EnumDifficulty[] BY_ID = (EnumDifficulty[]) Arrays.stream(values()).sorted(Comparator.comparingInt(EnumDifficulty::getId)).toArray((i) -> {
        return new EnumDifficulty[i];
    });
    private final int id;
    private final String key;

    private EnumDifficulty(int i, String s) {
        this.id = i;
        this.key = s;
    }

    public int getId() {
        return this.id;
    }

    public IChatBaseComponent getDisplayName() {
        return new ChatMessage("options.difficulty." + this.key);
    }

    public static EnumDifficulty byId(int i) {
        return EnumDifficulty.BY_ID[i % EnumDifficulty.BY_ID.length];
    }

    @Nullable
    public static EnumDifficulty byName(String s) {
        EnumDifficulty[] aenumdifficulty = values();
        int i = aenumdifficulty.length;

        for (int j = 0; j < i; ++j) {
            EnumDifficulty enumdifficulty = aenumdifficulty[j];

            if (enumdifficulty.key.equals(s)) {
                return enumdifficulty;
            }
        }

        return null;
    }

    public String getKey() {
        return this.key;
    }
}
