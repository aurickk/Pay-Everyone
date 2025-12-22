package pay.everyone.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.network.protocol.game.ClientboundCommandSuggestionsPacket;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class VersionCompat {
    private static Boolean isNewApi = null;
    private static Method getNameMethod = null;
    private static boolean versionDetected = false;
    
    private static Method packetIdMethod = null;
    private static Method packetSuggestionsMethod = null;
    private static Method suggestionTextMethod = null;
    private static boolean packetMethodsDetected = false;
    
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        if (!versionDetected) detectApiVersion();
        
        if (getNameMethod != null) {
            try {
                Object result = getNameMethod.invoke(profile);
                return result != null ? result.toString() : null;
            } catch (Exception e) {
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
    
    public static float getTickDelta(Object tickDelta) {
        if (tickDelta == null) return 0.0f;
        
        if (tickDelta instanceof Number) {
            return ((Number) tickDelta).floatValue();
        }
        
        try {
            Method method = tickDelta.getClass().getMethod("getRealtimeDeltaTicks");
            Object result = method.invoke(tickDelta);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
        
        try {
            Method method = tickDelta.getClass().getMethod("getTickDelta", boolean.class);
            Object result = method.invoke(tickDelta, true);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
        
        try {
            return Float.parseFloat(tickDelta.toString());
        } catch (Exception ignored) {}
        
        return 0.0f;
    }
    
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
        
        try {
            return packet.id();
        } catch (NoSuchMethodError ignored) {}
        
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
        
        try {
            return (List<ClientboundCommandSuggestionsPacket.Entry>) packet.suggestions();
        } catch (NoSuchMethodError ignored) {}
        
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
        
        try {
            return entry.text();
        } catch (NoSuchMethodError ignored) {}
        
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
