package pay.everyone.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.*;

/**
 * Version compatibility layer for Minecraft API differences.
 * MC 1.21.4-1.21.8: GameProfile.getName(), KeyMapping uses String category
 * MC 1.21.9+: GameProfile.name(), KeyMapping uses Category enum/record
 */
public class VersionCompat {
    private static Boolean isNewApi = null;
    private static Method getNameMethod = null;
    
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        if (isNewApi == null) detectApiVersion();
        
        try {
            if (getNameMethod != null) {
                Object result = getNameMethod.invoke(profile);
                return result != null ? result.toString() : null;
            }
        } catch (Exception e) {
            try {
                return (String) GameProfile.class.getMethod("name").invoke(profile);
            } catch (Exception e2) {
                try {
                    return (String) GameProfile.class.getMethod("getName").invoke(profile);
                } catch (Exception e3) {
                    return null;
                }
            }
        }
        return null;
    }
    
    private static void detectApiVersion() {
        try {
            getNameMethod = GameProfile.class.getMethod("name");
            isNewApi = true;
        } catch (NoSuchMethodException e) {
            try {
                getNameMethod = GameProfile.class.getMethod("getName");
                isNewApi = false;
            } catch (NoSuchMethodException e2) {
                isNewApi = false;
            }
        }
    }
    
    public static KeyMapping createKeyMapping(String translationKey, InputConstants.Type type, int keyCode, String categoryKey) {
        Constructor<?>[] constructors = KeyMapping.class.getConstructors();
        
        // Try 3-param constructor (String, int, Category)
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 3 && paramTypes[0] == String.class && paramTypes[1] == int.class) {
                Object categoryValue = getFirstEnumOrRecordValue(paramTypes[2]);
                if (categoryValue != null) {
                    try {
                        return (KeyMapping) constructor.newInstance(translationKey, keyCode, categoryValue);
                    } catch (Exception ignored) {}
                }
            }
        }
        
        // Try 4-param constructor (String, Type, int, Category)
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 4 && paramTypes[0] == String.class && paramTypes[2] == int.class) {
                Object categoryValue = getFirstEnumOrRecordValue(paramTypes[3]);
                if (categoryValue != null) {
                    try {
                        return (KeyMapping) constructor.newInstance(translationKey, type, keyCode, categoryValue);
                    } catch (Exception ignored) {}
                }
            }
        }
        
        // Fallback: String category constructors
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 4 && paramTypes[0] == String.class && paramTypes[2] == int.class && paramTypes[3] == String.class) {
                try {
                    return (KeyMapping) constructor.newInstance(translationKey, type, keyCode, categoryKey);
                } catch (Exception ignored) {}
            }
            if (paramTypes.length == 3 && paramTypes[0] == String.class && paramTypes[1] == int.class && paramTypes[2] == String.class) {
                try {
                    return (KeyMapping) constructor.newInstance(translationKey, keyCode, categoryKey);
                } catch (Exception ignored) {}
            }
        }
        
        throw new RuntimeException("Failed to create KeyMapping for version compatibility");
    }
    
    private static Object getFirstEnumOrRecordValue(Class<?> clazz) {
        if (clazz == null) return null;
        
        // Check if it's an enum or record
        Class<?> superclass = clazz.getSuperclass();
        boolean isRecord = false, isEnum = false;
        
        for (Class<?> current = superclass; current != null; current = current.getSuperclass()) {
            if (current.equals(Enum.class)) { isEnum = true; break; }
            if (current.equals(java.lang.Record.class)) { isRecord = true; break; }
        }
        
        String[] preferredNames = {"MISC", "GAMEPLAY", "MOVEMENT", "INVENTORY", "MULTIPLAYER"};
        
        if (isRecord) {
            return findStaticFieldValue(clazz, clazz, preferredNames);
        }
        
        if (isEnum) {
            // Try Enum.valueOf with common names
            for (String name : preferredNames) {
                try {
                    @SuppressWarnings("unchecked")
                    Object value = Enum.valueOf((Class<? extends Enum>) clazz, name);
                    return value;
                } catch (Exception ignored) {}
            }
            
            // Try getEnumConstants
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
            
            // Try field access
            return findStaticFieldValue(clazz, clazz, preferredNames);
        }
        
        return null;
    }
    
    private static Object findStaticFieldValue(Class<?> targetType, Class<?> searchIn, String[] preferredNames) {
        Object firstFound = null;
        
        // Search in the type's own class
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
        
        // Also search in KeyMapping class
        if (!searchIn.equals(KeyMapping.class)) {
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
}
