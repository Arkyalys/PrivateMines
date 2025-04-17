package fr.ju.privateMines.utils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
public class ColorUtil {
    private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
            .character('&')
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    public static Component deserialize(String text) {
        if (text == null) return Component.empty();
        return LEGACY_SERIALIZER.deserialize(text);
    }
    public static String serialize(Component component) {
        if (component == null) return "";
        return LEGACY_SERIALIZER.serialize(component);
    }
    public static String translateColors(String text) {
        if (text == null) return "";
        return text.replace('&', '§');
    }
} 