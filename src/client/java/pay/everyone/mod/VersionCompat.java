package pay.everyone.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;

<<<<<<< HEAD
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VersionCompat {
    private static Boolean isNewApi = null;
    private static Method getNameMethod = null;
    private static boolean versionDetected = false;
    
=======
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
>>>>>>> 230b532 (feat: migrate to stonecutter)
    private static Method packetIdMethod = null;
    private static Method packetSuggestionsMethod = null;
    private static Method suggestionTextMethod = null;
    private static boolean packetMethodsDetected = false;
    
<<<<<<< HEAD
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        if (!versionDetected) detectApiVersion();
=======
    // ===== GameProfile methods (reflection - authlib dependent) =====
    
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        if (!profileDetected) detectProfileMethod();
>>>>>>> 230b532 (feat: migrate to stonecutter)
        
        if (getNameMethod != null) {
            try {
                Object result = getNameMethod.invoke(profile);
                return result != null ? result.toString() : null;
            } catch (Exception e) {
<<<<<<< HEAD
                PayEveryone.LOGGER.debug("Failed to invoke cached getNameMethod, trying fallback", e);
            }
        }
        
        try {
            Method nameMethod = GameProfile.class.getMethod("name");
            Object result = nameMethod.invoke(profile);
            if (result != null) {
                getNameMethod = nameMethod;
                isNewApi = true;
                return result.toString();
            }
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            PayEveryone.LOGGER.debug("Failed to use name() method, trying legacy getName()", e);
        }
        
        try {
            Method legacyMethod = GameProfile.class.getMethod("getName");
            Object result = legacyMethod.invoke(profile);
            if (result != null) {
                getNameMethod = legacyMethod;
                isNewApi = false;
                return result.toString();
            }
        } catch (NoSuchMethodException e) {
            PayEveryone.LOGGER.error("Neither name() nor getName() method found on GameProfile.", e);
        } catch (Exception e) {
            PayEveryone.LOGGER.error("Failed to retrieve player name from GameProfile", e);
        }
        
        return null;
    }
    
    private static void detectApiVersion() {
        if (versionDetected) return;
        
        synchronized (VersionCompat.class) {
            if (versionDetected) return;
            
            try {
                getNameMethod = GameProfile.class.getMethod("name");
                isNewApi = true;
                PayEveryone.LOGGER.debug("Detected modern API (GameProfile.name()) - Minecraft 1.21.5+");
            } catch (NoSuchMethodException e) {
                try {
                    getNameMethod = GameProfile.class.getMethod("getName");
                    isNewApi = false;
                    PayEveryone.LOGGER.debug("Detected legacy API (GameProfile.getName()) - Minecraft 1.21.1-1.21.4");
                } catch (NoSuchMethodException e2) {
                    isNewApi = false;
                    PayEveryone.LOGGER.error("Could not detect Minecraft version API.");
                }
            }
            
            versionDetected = true;
        }
    }
    
    public static KeyMapping createKeyMapping(String translationKey, InputConstants.Type type, int keyCode, String categoryKey) {
        Constructor<?>[] constructors = KeyMapping.class.getConstructors();
        List<String> attemptedSignatures = new ArrayList<>();
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 4 && 
                paramTypes[0] == String.class && 
                paramTypes[1] == InputConstants.Type.class &&
                paramTypes[2] == int.class && 
                paramTypes[3] == String.class) {
                try {
                    return (KeyMapping) constructor.newInstance(translationKey, type, keyCode, categoryKey);
                } catch (Exception e) {
                    attemptedSignatures.add("(String, InputConstants.Type, int, String)");
                }
            }
        }
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 4 && 
                paramTypes[0] == String.class && 
                paramTypes[1] == InputConstants.Type.class &&
                paramTypes[2] == int.class) {
                Object categoryValue = getFirstEnumOrRecordValue(paramTypes[3]);
                if (categoryValue != null) {
                    try {
                        return (KeyMapping) constructor.newInstance(translationKey, type, keyCode, categoryValue);
                    } catch (Exception e) {
                        attemptedSignatures.add("(String, InputConstants.Type, int, " + paramTypes[3].getSimpleName() + ")");
                    }
                }
            }
        }
        
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 3 && paramTypes[0] == String.class && paramTypes[1] == int.class) {
                if (paramTypes[2] == String.class) {
                    try {
                        return (KeyMapping) constructor.newInstance(translationKey, keyCode, categoryKey);
                    } catch (Exception e) {
                        attemptedSignatures.add("(String, int, String)");
                    }
                }
                Object categoryValue = getFirstEnumOrRecordValue(paramTypes[2]);
                if (categoryValue != null) {
                    try {
                        return (KeyMapping) constructor.newInstance(translationKey, keyCode, categoryValue);
                    } catch (Exception e) {
                        attemptedSignatures.add("(String, int, " + paramTypes[2].getSimpleName() + ")");
                    }
                }
            }
        }
        
        String errorMsg = String.format(
            "Failed to create KeyMapping for version compatibility. Attempted signatures: %s",
            String.join(", ", attemptedSignatures)
        );
        PayEveryone.LOGGER.error(errorMsg);
        throw new RuntimeException(errorMsg);
    }
    
    private static Object getFirstEnumOrRecordValue(Class<?> clazz) {
        if (clazz == null || clazz == String.class) return null;
        
        Class<?> superclass = clazz.getSuperclass();
        boolean isRecord = false, isEnum = false;
        
        for (Class<?> current = superclass; current != null; current = current.getSuperclass()) {
            if (current == Enum.class) { isEnum = true; break; }
            if (current == java.lang.Record.class) { isRecord = true; break; }
        }
        
        String[] preferredNames = {"MISC", "GAMEPLAY", "MOVEMENT", "INVENTORY", "MULTIPLAYER"};
        
        if (isRecord) {
            return findStaticFieldValue(clazz, clazz, preferredNames);
        }
        
        if (isEnum) {
            for (String name : preferredNames) {
                try {
                    @SuppressWarnings({"unchecked", "rawtypes"})
                    Object value = Enum.valueOf((Class<? extends Enum>) clazz, name);
                    return value;
                } catch (Exception ignored) {}
            }
            
            try {
                Object[] constants = clazz.getEnumConstants();
                if (constants != null && constants.length > 0) {
                    for (Object c : constants) {
                        String name = ((Enum<?>) c).name();
                        for (String preferred : preferredNames) {
                            if (name.equals(preferred)) return c;
                        }
                    }
                    return constants[0];
                }
            } catch (Exception ignored) {}
            
            return findStaticFieldValue(clazz, clazz, preferredNames);
        }
        
        return null;
    }
    
    private static Object findStaticFieldValue(Class<?> targetType, Class<?> searchIn, String[] preferredNames) {
        Object firstFound = null;
        
        try {
            for (Field field : searchIn.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) 
                        && targetType.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    Object value = field.get(null);
                    if (value != null) {
                        if (firstFound == null) firstFound = value;
                        for (String name : preferredNames) {
                            if (field.getName().equalsIgnoreCase(name)) return value;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
        
        if (searchIn != KeyMapping.class) {
            try {
                for (Field field : KeyMapping.class.getDeclaredFields()) {
                    if (Modifier.isStatic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) 
                            && targetType.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if (value != null) {
                            if (firstFound == null) firstFound = value;
                            for (String name : preferredNames) {
                                if (field.getName().equalsIgnoreCase(name)) return value;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
        }
        
        return firstFound;
    }
    
    public static boolean isNewApi() {
        if (isNewApi == null) detectApiVersion();
        return isNewApi != null && isNewApi;
    }
    
=======
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
    
>>>>>>> 230b532 (feat: migrate to stonecutter)
    public static float getTickDelta(Object tickDelta) {
        if (tickDelta == null) return 0.0f;
        
        if (tickDelta instanceof Number) {
            return ((Number) tickDelta).floatValue();
        }
        
<<<<<<< HEAD
=======
        //? if >=1.21.6 {
>>>>>>> 230b532 (feat: migrate to stonecutter)
        try {
            Method method = tickDelta.getClass().getMethod("getRealtimeDeltaTicks");
            Object result = method.invoke(tickDelta);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
<<<<<<< HEAD
        
=======
        //? } else {
>>>>>>> 230b532 (feat: migrate to stonecutter)
        try {
            Method method = tickDelta.getClass().getMethod("getTickDelta", boolean.class);
            Object result = method.invoke(tickDelta, true);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
<<<<<<< HEAD
=======
        //?}
>>>>>>> 230b532 (feat: migrate to stonecutter)
        
        try {
            return Float.parseFloat(tickDelta.toString());
        } catch (Exception ignored) {}
        
        return 0.0f;
    }
    
<<<<<<< HEAD
=======
    // ===== Packet methods (reflection - method names vary) =====
    
>>>>>>> 230b532 (feat: migrate to stonecutter)
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
        
<<<<<<< HEAD
=======
        // Try direct access first (modern API)
>>>>>>> 230b532 (feat: migrate to stonecutter)
        try {
            return packet.id();
        } catch (NoSuchMethodError ignored) {}
        
<<<<<<< HEAD
=======
        // Fall back to reflection
>>>>>>> 230b532 (feat: migrate to stonecutter)
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
        
<<<<<<< HEAD
=======
        // Try direct access first (modern API)
>>>>>>> 230b532 (feat: migrate to stonecutter)
        try {
            return (List<ClientboundCommandSuggestionsPacket.Entry>) packet.suggestions();
        } catch (NoSuchMethodError ignored) {}
        
<<<<<<< HEAD
=======
        // Fall back to reflection
>>>>>>> 230b532 (feat: migrate to stonecutter)
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
        
<<<<<<< HEAD
=======
        // Try direct access first (modern API)
>>>>>>> 230b532 (feat: migrate to stonecutter)
        try {
            return entry.text();
        } catch (NoSuchMethodError ignored) {}
        
<<<<<<< HEAD
=======
        // Fall back to reflection
>>>>>>> 230b532 (feat: migrate to stonecutter)
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
