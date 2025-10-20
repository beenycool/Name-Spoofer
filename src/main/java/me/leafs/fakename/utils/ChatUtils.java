package me.leafs.fakename.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ChatUtils {
    private static final Pattern COLOR_FORMAT = Pattern.compile("[§&][0-9a-fk-or]", Pattern.CASE_INSENSITIVE);

    private ChatUtils() {
    }

    public static void printChat(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.inGameHud == null) {
            return;
        }

        String formatted = color(message);
        client.inGameHud.getChatHud().addMessage(Text.literal(formatted));
    }

    public static String color(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        Matcher matcher = COLOR_FORMAT.matcher(input);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            Formatting formatting = Formatting.byCode(fullMatch.charAt(1));
            String replacement = formatting == null ? fullMatch : formatting.toString();
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(replacement));
        }

        matcher.appendTail(buffer);
        return buffer.toString();
    }
}
