package pay.everyone.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * Version compatibility layer to handle API differences between Minecraft versions.
 * 
 * MC 1.21.4-1.21.8: GameProfile.getName(), KeyMapping uses String category
 * MC 1.21.9+: GameProfile.name(), KeyMapping uses Category enum
 */
public class VersionCompat {
    
    private static Boolean isNewApi = null;
    private static Method getNameMethod = null;
    
    /**
     * Get the player name from a GameProfile, handling API differences.
     * In 1.21.9+, getName() was changed to name().
     */
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        
        // Try to detect which API version we're on
        if (isNewApi == null) {
            detectApiVersion();
        }
        
        try {
            if (getNameMethod != null) {
                Object result = getNameMethod.invoke(profile);
                return result != null ? result.toString() : null;
            }
        } catch (Exception e) {
            // Fallback - try both methods
            try {
                Method method = GameProfile.class.getMethod("name");
                return (String) method.invoke(profile);
            } catch (Exception e2) {
                try {
                    Method method = GameProfile.class.getMethod("getName");
                    return (String) method.invoke(profile);
                } catch (Exception e3) {
                    return null;
                }
            }
        }
        return null;
    }
    
    /**
     * Detect which API version we're running on
     */
    private static void detectApiVersion() {
        // Try new API first (1.21.9+): name()
        try {
            getNameMethod = GameProfile.class.getMethod("name");
            isNewApi = true;
            return;
        } catch (NoSuchMethodException e) {
            // Not new API
        }
        
        // Try old API (1.21.4-1.21.8): getName()
        try {
            getNameMethod = GameProfile.class.getMethod("getName");
            isNewApi = false;
        } catch (NoSuchMethodException e) {
            // Neither found - shouldn't happen
            isNewApi = false;
        }
    }
    
    /**
     * Create a KeyMapping with version-appropriate category handling.
     * Dynamically finds and uses the appropriate constructor.
     */
    public static KeyMapping createKeyMapping(String translationKey, InputConstants.Type type, int keyCode, String categoryKey) {
        Constructor<?>[] constructors = KeyMapping.class.getConstructors();
        StringBuilder debug = new StringBuilder();
        
        debug.append("Found ").append(constructors.length).append(" constructors. ");
        
        // Try 3-parameter constructor first (String, int, Category) - simpler
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            
            if (paramTypes.length == 3 && paramTypes[0] == String.class && paramTypes[1] == int.class) {
                debug.append("Found 3-param: String, int, ").append(paramTypes[2].getName()).append(". ");
                
                // Try to get enum value (don't check isEnum() as it may fail for obfuscated inner classes)
                debug.append("Attempting to get enum value... ");
                try {
                    Object enumValue = getFirstEnumValue(paramTypes[2], debug);
                    if (enumValue != null) {
                        debug.append("Got enum: ").append(enumValue.toString()).append(". ");
                        try {
                            return (KeyMapping) constructor.newInstance(translationKey, keyCode, enumValue);
                        } catch (Exception e) {
                            debug.append("3-param newInstance FAILED: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
                            if (e.getCause() != null) {
                                debug.append("Cause: ").append(e.getCause().getClass().getSimpleName()).append(": ").append(e.getCause().getMessage()).append(". ");
                            }
                        }
                    } else {
                        debug.append("getFirstEnumValue returned null. ");
                    }
                } catch (Exception e) {
                    debug.append("getFirstEnumValue threw: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
                }
            }
        }
        
        // Try 4-parameter constructor (String, Type, int, Category)
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            
            if (paramTypes.length == 4 && paramTypes[0] == String.class && paramTypes[2] == int.class) {
                debug.append("Found 4-param: String, ").append(paramTypes[1].getName()).append(", int, ").append(paramTypes[3].getName()).append(". ");
                
                // Try to get enum value (don't check isEnum() as it may fail for obfuscated inner classes)
                debug.append("Attempting to get enum value... ");
                try {
                    Object enumValue = getFirstEnumValue(paramTypes[3], debug);
                    if (enumValue != null) {
                        debug.append("Got enum: ").append(enumValue.toString()).append(". ");
                        try {
                            return (KeyMapping) constructor.newInstance(translationKey, type, keyCode, enumValue);
                        } catch (Exception e) {
                            debug.append("4-param newInstance FAILED: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
                            if (e.getCause() != null) {
                                debug.append("Cause: ").append(e.getCause().getClass().getSimpleName()).append(": ").append(e.getCause().getMessage()).append(". ");
                            }
                        }
                    } else {
                        debug.append("getFirstEnumValue returned null. ");
                    }
                } catch (Exception e) {
                    debug.append("getFirstEnumValue threw: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
                }
            }
        }
        
        // Fallback: Try String category constructors (for older versions or if enum failed)
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            
            if (paramTypes.length == 4 && paramTypes[0] == String.class && paramTypes[2] == int.class && paramTypes[3] == String.class) {
                debug.append("Found String 4-param: String, ").append(paramTypes[1].getName()).append(", int, String. ");
                try {
                    return (KeyMapping) constructor.newInstance(translationKey, type, keyCode, categoryKey);
                } catch (Exception e) {
                    debug.append("String 4-param FAILED: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
                }
            }
            
            if (paramTypes.length == 3 && paramTypes[0] == String.class && paramTypes[1] == int.class && paramTypes[2] == String.class) {
                debug.append("Found String 3-param: String, int, String. ");
                try {
                    return (KeyMapping) constructor.newInstance(translationKey, keyCode, categoryKey);
                } catch (Exception e) {
                    debug.append("String 3-param FAILED: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
                }
            }
        }
        
        // Last resort: throw with details
        throw new RuntimeException("Failed to create KeyMapping. Debug: " + debug);
    }
    
    /**
     * Get the first enum/record value from a class, preferring MISC/GAMEPLAY if available.
     * Handles both enum types and record types (for 1.21.10+ where Category might be a record).
     */
    private static Object getFirstEnumValue(Class<?> enumClass, StringBuilder debug) {
        if (enumClass == null) {
            debug.append("Class is null. ");
            return null;
        }
        
        debug.append("Getting value from ").append(enumClass.getName()).append(" (isEnum()=").append(enumClass.isEnum()).append(", superclass=").append(enumClass.getSuperclass() != null ? enumClass.getSuperclass().getName() : "null").append("). ");
        
        // Check if it's a record (extends Record)
        Class<?> superclass = enumClass.getSuperclass();
        boolean isRecord = false;
        boolean isEnumType = false;
        
        if (superclass != null) {
            Class<?> current = superclass;
            while (current != null) {
                if (current.equals(Enum.class)) {
                    isEnumType = true;
                    break;
                }
                if (current.equals(java.lang.Record.class)) {
                    isRecord = true;
                    break;
                }
                current = current.getSuperclass();
            }
        }
        
        // If it's a record, try to find static fields that are instances of this record
        if (isRecord) {
            debug.append("Class is a Record, searching for static fields. ");
            Object firstFound = null;
            
            // First, check the record class itself
            try {
                java.lang.reflect.Field[] fields = enumClass.getDeclaredFields();
                debug.append("Found ").append(fields.length).append(" declared fields in record class. ");
                for (java.lang.reflect.Field field : fields) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                            java.lang.reflect.Modifier.isFinal(field.getModifiers()) &&
                            enumClass.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            Object value = field.get(null);
                            if (value != null) {
                                if (firstFound == null) {
                                    firstFound = value;
                                }
                                String fieldName = field.getName();
                                debug.append("Found record via field: ").append(fieldName).append(". ");
                                if (fieldName.equalsIgnoreCase("MISC") || fieldName.equalsIgnoreCase("GAMEPLAY") || fieldName.equalsIgnoreCase("MOVEMENT")) {
                                    return value;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Skip this field
                    }
                }
            } catch (Exception e) {
                debug.append("Field access in record class failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
            }
            
            // Also check KeyMapping class for static fields of this type
            try {
                java.lang.reflect.Field[] keyMappingFields = KeyMapping.class.getDeclaredFields();
                debug.append("Found ").append(keyMappingFields.length).append(" declared fields in KeyMapping. ");
                for (java.lang.reflect.Field field : keyMappingFields) {
                    try {
                        if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                            java.lang.reflect.Modifier.isFinal(field.getModifiers()) &&
                            enumClass.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);
                            Object value = field.get(null);
                            if (value != null) {
                                if (firstFound == null) {
                                    firstFound = value;
                                }
                                String fieldName = field.getName();
                                debug.append("Found record via KeyMapping field: ").append(fieldName).append(". ");
                                if (fieldName.equalsIgnoreCase("MISC") || fieldName.equalsIgnoreCase("GAMEPLAY") || fieldName.equalsIgnoreCase("MOVEMENT")) {
                                    return value;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // Skip this field
                    }
                }
            } catch (Exception e) {
                debug.append("Field access in KeyMapping failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
            }
            
            if (firstFound != null) {
                debug.append("Using first record field found. ");
                return firstFound;
            }
            
            debug.append("No record fields found. ");
            return null;
        }
        
        // If it's not an enum either, return null
        if (!isEnumType) {
            debug.append("Class is neither Enum nor Record. ");
            return null;
        }
        
        // Method 1: Try Enum.valueOf() directly with common names
        String[] commonNames = {"MISC", "GAMEPLAY", "MOVEMENT", "INVENTORY", "MULTIPLAYER"};
        for (String name : commonNames) {
            try {
                @SuppressWarnings("unchecked")
                Object enumValue = Enum.valueOf((Class<? extends Enum>) enumClass, name);
                debug.append("Enum.valueOf(").append(name).append(") succeeded. ");
                return enumValue;
            } catch (Exception e) {
                // Try next name
            }
        }
        
        // Method 2: Try getEnumConstants()
        try {
            Object[] enumConstants = enumClass.getEnumConstants();
            if (enumConstants != null && enumConstants.length > 0) {
                debug.append("getEnumConstants() found ").append(enumConstants.length).append(" values. ");
                
                // Look for preferred values
                for (Object enumConstant : enumConstants) {
                    try {
                        String name = ((Enum<?>) enumConstant).name();
                        if (name.equals("MISC") || name.equals("GAMEPLAY") || name.equals("MOVEMENT")) {
                            debug.append("Using ").append(name).append(". ");
                            return enumConstant;
                        }
                    } catch (Exception e) {
                        // Skip
                    }
                }
                
                // Return first
                Object first = enumConstants[0];
                try {
                    String name = ((Enum<?>) first).name();
                    debug.append("Using first enum: ").append(name).append(". ");
                } catch (Exception e) {
                    debug.append("Using first enum (name unknown). ");
                }
                return first;
            } else {
                debug.append("getEnumConstants() returned null or empty. ");
            }
        } catch (Exception e) {
            debug.append("getEnumConstants() failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
        }
        
        // Method 3: Try getDeclaredFields() to find enum constants
        try {
            java.lang.reflect.Field[] fields = enumClass.getDeclaredFields();
            debug.append("Found ").append(fields.length).append(" declared fields. ");
            Object firstFound = null;
            for (java.lang.reflect.Field field : fields) {
                try {
                    if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) && 
                        java.lang.reflect.Modifier.isFinal(field.getModifiers()) &&
                        enumClass.isAssignableFrom(field.getType())) {
                        field.setAccessible(true);
                        Object value = field.get(null);
                        if (value != null) {
                            if (firstFound == null) {
                                firstFound = value;
                            }
                            try {
                                String name = ((Enum<?>) value).name();
                                debug.append("Found enum via field: ").append(field.getName()).append(" (").append(name).append("). ");
                                if (name.equals("MISC") || name.equals("GAMEPLAY") || name.equals("MOVEMENT")) {
                                    return value;
                                }
                            } catch (Exception e) {
                                // Continue
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip this field
                }
            }
            if (firstFound != null) {
                debug.append("Using first field-found enum. ");
                return firstFound;
            }
        } catch (Exception e) {
            debug.append("Field access failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
        }
        
        // Method 4: Try accessing values() method via reflection
        try {
            Method valuesMethod = enumClass.getDeclaredMethod("values");
            valuesMethod.setAccessible(true);
            Object[] values = (Object[]) valuesMethod.invoke(null);
            if (values != null && values.length > 0) {
                debug.append("values() found ").append(values.length).append(" values. ");
                return values[0];
            }
        } catch (Exception e) {
            debug.append("values() failed: ").append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append(". ");
        }
        
        debug.append("All enum retrieval methods failed. ");
        return null;
    }
    
    /**
     * Check if we're running on the new API (1.21.9+)
     */
    public static boolean isNewApi() {
        if (isNewApi == null) {
            detectApiVersion();
        }
        return isNewApi != null && isNewApi;
    }
}
