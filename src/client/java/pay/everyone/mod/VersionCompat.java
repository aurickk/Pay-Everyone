package pay.everyone.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Version compatibility utilities for Pay Everyone mod.
 * Uses Stonecutter compile-time conditionals for known API differences.
 * Keeps reflection for GameProfile (authlib) and packet methods (version-dependent).
 */
public class VersionCompat {
    // GameProfile reflection - authlib version varies independently of MC version
    private static Method getNameMethod = null;
    private static boolean profileDetected = false;
    
    // Packet methods reflection - method names vary between MC versions
    private static Method packetIdMethod = null;
    private static Method packetSuggestionsMethod = null;
    private static Method suggestionTextMethod = null;
    private static boolean packetMethodsDetected = false;
    
    // ===== GameProfile methods (reflection - authlib dependent) =====
    
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        if (!profileDetected) detectProfileMethod();
        
        if (getNameMethod != null) {
            try {
                Object result = getNameMethod.invoke(profile);
                return result != null ? result.toString() : null;
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Failed to invoke cached getNameMethod", e);
            }
        }
        return null;
    }
    
    private static void detectProfileMethod() {
        if (profileDetected) return;
        
        synchronized (VersionCompat.class) {
            if (profileDetected) return;
            
            try {
                getNameMethod = GameProfile.class.getMethod("name");
            } catch (NoSuchMethodException e) {
                try {
                    getNameMethod = GameProfile.class.getMethod("getName");
                } catch (NoSuchMethodException e2) {
                    PayEveryone.LOGGER.error("Could not detect GameProfile API");
                }
            }
            
            profileDetected = true;
        }
    }
    
    // ===== KeyMapping (API changed across versions) =====
    // 1.21.1-1.21.5: 4-arg constructor with InputConstants.Type
    // 1.21.6-1.21.8: 3-arg constructor with String category
    // 1.21.9+: 3-arg constructor with KeyMapping.Category
    
    //? if >=1.21.9 {
    private static KeyMapping.Category cachedCategory = null;
    //? }
    
    public static KeyMapping createKeyMapping(String translationKey, InputConstants.Type type, int keyCode, String categoryKey) {
        //? if <1.21.6 {
        return new KeyMapping(translationKey, type, keyCode, categoryKey);
        //? } else {
        //? if <1.21.9 {
        return new KeyMapping(translationKey, keyCode, categoryKey);
        //? } else {
        if (cachedCategory == null) {
            cachedCategory = KeyMapping.Category.register(net.minecraft.resources.ResourceLocation.parse("pay-everyone:main"));
        }
        return new KeyMapping(translationKey, keyCode, cachedCategory);
        //? }
        //? }
    }
    
    // ===== API version check (compile-time constant) =====
    
    public static boolean isNewApi() {
        //? if >=1.21.6 {
        return true;
        //? } else {
        return false;
        //?}
    }
    
    // ===== Tick delta (compile-time conditional for method name) =====
    
    public static float getTickDelta(Object tickDelta) {
        if (tickDelta == null) return 0.0f;
        
        if (tickDelta instanceof Number) {
            return ((Number) tickDelta).floatValue();
        }
        
        //? if >=1.21.6 {
        try {
            Method method = tickDelta.getClass().getMethod("getRealtimeDeltaTicks");
            Object result = method.invoke(tickDelta);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
        //? } else {
        try {
            Method method = tickDelta.getClass().getMethod("getTickDelta", boolean.class);
            Object result = method.invoke(tickDelta, true);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
        //?}
        
        try {
            return Float.parseFloat(tickDelta.toString());
        } catch (Exception ignored) {}
        
        return 0.0f;
    }
    
    // ===== Packet methods (reflection - method names vary) =====
    
    private static void detectPacketMethods() {
        if (packetMethodsDetected) return;
        
        synchronized (VersionCompat.class) {
            if (packetMethodsDetected) return;
            
            try {
                packetIdMethod = ClientboundCommandSuggestionsPacket.class.getMethod("id");
            } catch (NoSuchMethodException e) {
                try {
                    packetIdMethod = ClientboundCommandSuggestionsPacket.class.getMethod("getId");
                } catch (NoSuchMethodException e2) {
                    PayEveryone.LOGGER.debug("Could not find packet ID method");
                }
            }
            
            try {
                packetSuggestionsMethod = ClientboundCommandSuggestionsPacket.class.getMethod("suggestions");
            } catch (NoSuchMethodException e) {
                try {
                    packetSuggestionsMethod = ClientboundCommandSuggestionsPacket.class.getMethod("getSuggestions");
                } catch (NoSuchMethodException e2) {
                    PayEveryone.LOGGER.debug("Could not find packet suggestions method");
                }
            }
            
            for (Class<?> innerClass : ClientboundCommandSuggestionsPacket.class.getDeclaredClasses()) {
                if (innerClass.getSimpleName().equals("Entry")) {
                    try {
                        suggestionTextMethod = innerClass.getMethod("text");
                    } catch (NoSuchMethodException e) {
                        try {
                            suggestionTextMethod = innerClass.getMethod("getText");
                        } catch (NoSuchMethodException e2) {
                            PayEveryone.LOGGER.debug("Could not find Entry text method");
                        }
                    }
                    break;
                }
            }
            
            packetMethodsDetected = true;
        }
    }
    
    public static int getPacketId(ClientboundCommandSuggestionsPacket packet) {
        if (packet == null) return -1;
        detectPacketMethods();
        
        // Try direct access first (modern API)
        try {
            return packet.id();
        } catch (NoSuchMethodError ignored) {}
        
        // Fall back to reflection
        if (packetIdMethod != null) {
            try {
                Object result = packetIdMethod.invoke(packet);
                if (result instanceof Number) {
                    return ((Number) result).intValue();
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Failed to get packet ID via reflection", e);
            }
        }
        
        return -1;
    }
    
    @SuppressWarnings("unchecked")
    public static List<ClientboundCommandSuggestionsPacket.Entry> getPacketSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (packet == null) return Collections.emptyList();
        detectPacketMethods();
        
        // Try direct access first (modern API)
        try {
            return (List<ClientboundCommandSuggestionsPacket.Entry>) packet.suggestions();
        } catch (NoSuchMethodError ignored) {}
        
        // Fall back to reflection
        if (packetSuggestionsMethod != null) {
            try {
                Object result = packetSuggestionsMethod.invoke(packet);
                if (result instanceof List) {
                    return (List<ClientboundCommandSuggestionsPacket.Entry>) result;
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Failed to get packet suggestions via reflection", e);
            }
        }
        
        return Collections.emptyList();
    }
    
    public static String getSuggestionText(ClientboundCommandSuggestionsPacket.Entry entry) {
        if (entry == null) return null;
        detectPacketMethods();
        
        // Try direct access first (modern API)
        try {
            return entry.text();
        } catch (NoSuchMethodError ignored) {}
        
        // Fall back to reflection
        if (suggestionTextMethod != null) {
            try {
                Object result = suggestionTextMethod.invoke(entry);
                return result != null ? result.toString() : null;
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Failed to get suggestion text via reflection", e);
            }
        }
        
        return null;
    }
}
