package me.leafs.fakename.utils;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.util.Formatting;

public final class NameUtils {
    private NameUtils() {
    }

    public static String apply(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Map<String, String> targets = new LinkedHashMap<>(SpoofStorage.getTargets());
        if (targets.isEmpty()) {
            return input;
        }

        String result = input;
        for (Map.Entry<String, String> entry : targets.entrySet()) {
            String realName = entry.getKey();
            if (realName == null || realName.isEmpty() || !result.contains(realName)) {
                continue;
            }

            String fakeName = entry.getValue();
            if (fakeName == null) {
                fakeName = "";
            }

            String colored = ChatUtils.color(fakeName);
            if (!colored.equals(fakeName) && !colored.isEmpty()) {
                colored += Formatting.RESET;
            }

            result = result.replace(realName, colored);
        }

        return result;
    }
}
