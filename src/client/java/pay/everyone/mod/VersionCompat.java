package pay.everyone.mod;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
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
    
    /**
     * Gets the player name from a GameProfile, handling version differences.
     * Supports both getName() (1.21.1-1.21.4) and name() (1.21.5+) methods.
     * 
     * @param profile The GameProfile to get the name from
     * @return The player name, or null if profile is null or name cannot be retrieved
     */
    public static String getProfileName(GameProfile profile) {
        if (profile == null) return null;
        if (!versionDetected) detectApiVersion();
        
        // Try cached method first (fast path)
        if (getNameMethod != null) {
            try {
                Object result = getNameMethod.invoke(profile);
                return result != null ? result.toString() : null;
            } catch (Exception e) {
                // Method exists but invocation failed, try fallback
                PayEveryone.LOGGER.debug("Failed to invoke cached getNameMethod, trying fallback", e);
            }
        }
        
        // Fallback chain: try modern API first, then legacy
        try {
            Method nameMethod = GameProfile.class.getMethod("name");
            Object result = nameMethod.invoke(profile);
            if (result != null) {
                // Cache for future use
                getNameMethod = nameMethod;
                isNewApi = true;
                return result.toString();
            }
        } catch (NoSuchMethodException e) {
            // Modern API not available, try legacy
        } catch (Exception e) {
            PayEveryone.LOGGER.debug("Failed to use name() method, trying legacy getName()", e);
        }
        
        try {
            Method legacyMethod = GameProfile.class.getMethod("getName");
            Object result = legacyMethod.invoke(profile);
            if (result != null) {
                // Cache for future use
                getNameMethod = legacyMethod;
                isNewApi = false;
                return result.toString();
            }
        } catch (NoSuchMethodException e) {
            PayEveryone.LOGGER.error("Neither name() nor getName() method found on GameProfile. This should not happen.", e);
        } catch (Exception e) {
            PayEveryone.LOGGER.error("Failed to retrieve player name from GameProfile", e);
        }
        
        return null;
    }
    
    /**
     * Detects the Minecraft version API by checking which methods are available.
     * This is called once and caches the result for performance.
     */
    private static void detectApiVersion() {
        if (versionDetected) return;
        
        synchronized (VersionCompat.class) {
            if (versionDetected) return; // Double-check after acquiring lock
            
            try {
                // Try modern API first (1.21.5+)
                getNameMethod = GameProfile.class.getMethod("name");
                isNewApi = true;
                PayEveryone.LOGGER.debug("Detected modern API (GameProfile.name()) - Minecraft 1.21.5+");
            } catch (NoSuchMethodException e) {
                try {
                    // Fall back to legacy API (1.21.1-1.21.4)
                    getNameMethod = GameProfile.class.getMethod("getName");
                    isNewApi = false;
                    PayEveryone.LOGGER.debug("Detected legacy API (GameProfile.getName()) - Minecraft 1.21.1-1.21.4");
                } catch (NoSuchMethodException e2) {
                    // Neither method found - this should never happen
                    isNewApi = false;
                    PayEveryone.LOGGER.error("Could not detect Minecraft version API. Neither name() nor getName() found on GameProfile.");
                }
            }
            
            versionDetected = true;
        }
    }
    
    /**
     * Creates a KeyMapping instance compatible with all Minecraft versions 1.21.1-1.21.10.
     * 
     * Version differences:
     * - Legacy (1.21.1-1.21.4): Uses String category parameter
     * - Modern (1.21.5-1.21.10): Uses enum/record Category type
     * 
     * This method tries multiple constructor signatures to find the compatible one.
     * 
     * @param translationKey The translation key for the key binding
     * @param type The input type (KEYSYM, MOUSE, etc.)
     * @param keyCode The key code
     * @param categoryKey The category key (used for legacy versions or as fallback)
     * @return A KeyMapping instance
     * @throws RuntimeException if no compatible constructor is found
     */
    public static KeyMapping createKeyMapping(String translationKey, InputConstants.Type type, int keyCode, String categoryKey) {
        Constructor<?>[] constructors = KeyMapping.class.getConstructors();
        List<String> attemptedSignatures = new ArrayList<>();
        
        // Try the standard 4-parameter constructor with String category first (most common for 1.21.1-1.21.4)
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
        
        // Try modern API (1.21.5+): enum/record category with 4 parameters
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
        
        // Try 3-parameter constructors
        for (Constructor<?> constructor : constructors) {
            Class<?>[] paramTypes = constructor.getParameterTypes();
            if (paramTypes.length == 3 && paramTypes[0] == String.class && paramTypes[1] == int.class) {
                // Try with String category
                if (paramTypes[2] == String.class) {
                    try {
                        return (KeyMapping) constructor.newInstance(translationKey, keyCode, categoryKey);
                    } catch (Exception e) {
                        attemptedSignatures.add("(String, int, String)");
                    }
                }
                // Try with enum/record category
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
        
        // If we get here, no compatible constructor was found
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
    
    // Cached drawString method
    private static Method cachedDrawStringMethod = null;
    private static int drawStringMethodType = 0; // 0=unknown, 1=int coords, 2=float coords, 3=no shadow
    private static boolean drawStringMethodDetected = false;
    
    /**
     * Draws a string on screen with version compatibility.
     * Handles API differences in GuiGraphics.drawString between versions.
     * 
     * @param graphics The GuiGraphics context
     * @param font The font to use
     * @param text The text to draw
     * @param x X position
     * @param y Y position
     * @param color Text color
     * @param shadow Whether to draw shadow
     */
    public static void drawString(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) {
        if (graphics == null || font == null || text == null) return;
        
        // Use cached method if available
        if (drawStringMethodDetected && cachedDrawStringMethod != null) {
            try {
                invokeDrawString(graphics, font, text, x, y, color, shadow);
                return;
            } catch (Exception e) {
                // Cache invalid, re-detect
                drawStringMethodDetected = false;
                cachedDrawStringMethod = null;
            }
        }
        
        // Try direct method call first - most common signature (int coords with shadow)
        try {
            graphics.drawString(font, text, x, y, color, shadow);
            // Cache this approach
            drawStringMethodType = 1;
            drawStringMethodDetected = true;
            return;
        } catch (NoSuchMethodError ignored) {
            // Method signature doesn't match, try alternatives
        }
        
        // Try without shadow parameter
        try {
            graphics.drawString(font, text, x, y, color);
            drawStringMethodType = 3;
            drawStringMethodDetected = true;
            return;
        } catch (NoSuchMethodError ignored) {
            // Continue to reflection fallback
        }
        
        // Detect and cache the correct method via reflection
        detectDrawStringMethod();
        
        if (cachedDrawStringMethod != null) {
            try {
                invokeDrawString(graphics, font, text, x, y, color, shadow);
            } catch (Exception e) {
                PayEveryone.LOGGER.warn("drawString invocation failed: " + e.getMessage());
            }
        }
    }
    
    private static void detectDrawStringMethod() {
        if (drawStringMethodDetected) return;
        
        synchronized (VersionCompat.class) {
            if (drawStringMethodDetected) return;
            
            // Try all possible method signatures
            Method[] methods = GuiGraphics.class.getMethods();
            for (Method m : methods) {
                if (!m.getName().equals("drawString")) continue;
                
                Class<?>[] params = m.getParameterTypes();
                
                // Look for (Font, String, int, int, int, boolean)
                if (params.length == 6 && params[0] == Font.class && params[1] == String.class
                    && params[2] == int.class && params[3] == int.class 
                    && params[4] == int.class && params[5] == boolean.class) {
                    cachedDrawStringMethod = m;
                    drawStringMethodType = 1;
                    PayEveryone.LOGGER.debug("Found drawString(Font, String, int, int, int, boolean)");
                    break;
                }
                
                // Look for (Font, String, float, float, int, boolean)
                if (params.length == 6 && params[0] == Font.class && params[1] == String.class
                    && params[2] == float.class && params[3] == float.class 
                    && params[4] == int.class && params[5] == boolean.class) {
                    cachedDrawStringMethod = m;
                    drawStringMethodType = 2;
                    PayEveryone.LOGGER.debug("Found drawString(Font, String, float, float, int, boolean)");
                    break;
                }
                
                // Look for (Font, String, int, int, int) - no shadow
                if (params.length == 5 && params[0] == Font.class && params[1] == String.class
                    && params[2] == int.class && params[3] == int.class && params[4] == int.class) {
                    cachedDrawStringMethod = m;
                    drawStringMethodType = 3;
                    PayEveryone.LOGGER.debug("Found drawString(Font, String, int, int, int)");
                    break;
                }
                
                // Look for (Font, String, float, float, int) - float no shadow
                if (params.length == 5 && params[0] == Font.class && params[1] == String.class
                    && params[2] == float.class && params[3] == float.class && params[4] == int.class) {
                    cachedDrawStringMethod = m;
                    drawStringMethodType = 4;
                    PayEveryone.LOGGER.debug("Found drawString(Font, String, float, float, int)");
                    break;
                }
            }
            
            if (cachedDrawStringMethod == null) {
                // Log all available drawString methods for debugging
                PayEveryone.LOGGER.warn("Could not find compatible drawString method. Available methods:");
                for (Method m : methods) {
                    if (m.getName().equals("drawString")) {
                        StringBuilder sb = new StringBuilder("  drawString(");
                        Class<?>[] params = m.getParameterTypes();
                        for (int i = 0; i < params.length; i++) {
                            if (i > 0) sb.append(", ");
                            sb.append(params[i].getSimpleName());
                        }
                        sb.append(")");
                        PayEveryone.LOGGER.warn(sb.toString());
                    }
                }
            }
            
            drawStringMethodDetected = true;
        }
    }
    
    private static void invokeDrawString(GuiGraphics graphics, Font font, String text, int x, int y, int color, boolean shadow) throws Exception {
        if (cachedDrawStringMethod == null) return;
        
        switch (drawStringMethodType) {
            case 1: // int coords with shadow
                cachedDrawStringMethod.invoke(graphics, font, text, x, y, color, shadow);
                break;
            case 2: // float coords with shadow
                cachedDrawStringMethod.invoke(graphics, font, text, (float)x, (float)y, color, shadow);
                break;
            case 3: // int coords, no shadow
                cachedDrawStringMethod.invoke(graphics, font, text, x, y, color);
                break;
            case 4: // float coords, no shadow
                cachedDrawStringMethod.invoke(graphics, font, text, (float)x, (float)y, color);
                break;
        }
    }
    
    /**
     * Gets the tick delta value from the HudRenderCallback parameter.
     * Handles API differences between versions:
     * - 1.21.1-1.21.4: Float parameter directly
     * - 1.21.5+: DeltaTracker object with getRealtimeDeltaTicks() method
     * 
     * @param tickDelta The tick delta parameter from HudRenderCallback
     * @return The tick delta as a float
     */
    public static float getTickDelta(Object tickDelta) {
        if (tickDelta == null) return 0.0f;
        
        // If it's already a number, just return it
        if (tickDelta instanceof Number) {
            return ((Number) tickDelta).floatValue();
        }
        
        // Try to call getRealtimeDeltaTicks() for newer API
        try {
            Method method = tickDelta.getClass().getMethod("getRealtimeDeltaTicks");
            Object result = method.invoke(tickDelta);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
        
        // Try getTickDelta() as fallback
        try {
            Method method = tickDelta.getClass().getMethod("getTickDelta", boolean.class);
            Object result = method.invoke(tickDelta, true);
            if (result instanceof Number) {
                return ((Number) result).floatValue();
            }
        } catch (Exception ignored) {}
        
        // Last resort: try to convert directly
        try {
            return Float.parseFloat(tickDelta.toString());
        } catch (Exception ignored) {}
        
        return 0.0f;
    }
    
    // ============== Packet Compatibility Methods ==============
    
    /**
     * Detect and cache packet accessor methods.
     */
    private static void detectPacketMethods() {
        if (packetMethodsDetected) return;
        
        synchronized (VersionCompat.class) {
            if (packetMethodsDetected) return;
            
            try {
                // Try to find id() method
                packetIdMethod = ClientboundCommandSuggestionsPacket.class.getMethod("id");
            } catch (NoSuchMethodException e) {
                try {
                    packetIdMethod = ClientboundCommandSuggestionsPacket.class.getMethod("getId");
                } catch (NoSuchMethodException e2) {
                    PayEveryone.LOGGER.debug("Could not find packet ID method");
                }
            }
            
            // Try to find suggestions() method
            try {
                packetSuggestionsMethod = ClientboundCommandSuggestionsPacket.class.getMethod("suggestions");
            } catch (NoSuchMethodException e) {
                try {
                    packetSuggestionsMethod = ClientboundCommandSuggestionsPacket.class.getMethod("getSuggestions");
                } catch (NoSuchMethodException e2) {
                    PayEveryone.LOGGER.debug("Could not find packet suggestions method");
                }
            }
            
            // Try to find Entry.text() method
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
    
    /**
     * Gets the transaction ID from a command suggestions packet.
     */
    public static int getPacketId(ClientboundCommandSuggestionsPacket packet) {
        if (packet == null) return -1;
        detectPacketMethods();
        
        // Try direct method call first
        try {
            return packet.id();
        } catch (NoSuchMethodError ignored) {}
        
        // Use cached method
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
    
    /**
     * Gets the suggestions list from a command suggestions packet.
     */
    @SuppressWarnings("unchecked")
    public static List<ClientboundCommandSuggestionsPacket.Entry> getPacketSuggestions(ClientboundCommandSuggestionsPacket packet) {
        if (packet == null) return Collections.emptyList();
        detectPacketMethods();
        
        // Try direct method call first
        try {
            return (List<ClientboundCommandSuggestionsPacket.Entry>) packet.suggestions();
        } catch (NoSuchMethodError ignored) {}
        
        // Use cached method
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
    
    /**
     * Gets the text from a suggestion entry.
     */
    public static String getSuggestionText(ClientboundCommandSuggestionsPacket.Entry entry) {
        if (entry == null) return null;
        detectPacketMethods();
        
        // Try direct method call first
        try {
            return entry.text();
        } catch (NoSuchMethodError ignored) {}
        
        // Use cached method
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
    
    // ============== Graphics Compatibility Methods ==============
    
    /**
     * Enable scissor region with version compatibility.
     */
    public static void enableScissor(GuiGraphics graphics, int x1, int y1, int x2, int y2) {
        if (graphics == null) return;
        
        try {
            graphics.enableScissor(x1, y1, x2, y2);
        } catch (NoSuchMethodError e) {
            // Try alternative method names
            try {
                Method method = GuiGraphics.class.getMethod("setScissor", int.class, int.class, int.class, int.class);
                method.invoke(graphics, x1, y1, x2, y2);
            } catch (Exception ex) {
                PayEveryone.LOGGER.debug("Could not enable scissor", ex);
            }
        }
    }
    
    /**
     * Disable scissor region with version compatibility.
     */
    public static void disableScissor(GuiGraphics graphics) {
        if (graphics == null) return;
        
        try {
            graphics.disableScissor();
        } catch (NoSuchMethodError e) {
            // Try alternative method names
            try {
                Method method = GuiGraphics.class.getMethod("clearScissor");
                method.invoke(graphics);
            } catch (Exception ex) {
                PayEveryone.LOGGER.debug("Could not disable scissor", ex);
            }
        }
    }
    
    /**
     * Fill a rectangle with version compatibility.
     */
    public static void fill(GuiGraphics graphics, int x1, int y1, int x2, int y2, int color) {
        if (graphics == null) return;
        
        try {
            graphics.fill(x1, y1, x2, y2, color);
        } catch (NoSuchMethodError e) {
            // Try alternative method names or signatures
            try {
                Method method = GuiGraphics.class.getMethod("fill", int.class, int.class, int.class, int.class, int.class);
                method.invoke(graphics, x1, y1, x2, y2, color);
            } catch (Exception ex) {
                PayEveryone.LOGGER.debug("Could not fill rectangle", ex);
            }
        }
    }
    
    // ============== PoseStack Compatibility Methods ==============
    
    // Cached pose stack methods
    private static Method poseMethod = null;
    private static Method pushPoseMethod = null;
    private static Method popPoseMethod = null;
    private static Method translateMethod = null;
    private static Method scaleMethod = null;
    private static boolean poseMethodsDetected = false;
    
    /**
     * Detect and cache pose stack methods.
     * In 1.21.6+, pose() returns Matrix3x2fStack with different method names (push/pop vs pushPose/popPose).
     */
    private static void detectPoseMethods(GuiGraphics graphics) {
        if (poseMethodsDetected) return;
        
        synchronized (VersionCompat.class) {
            if (poseMethodsDetected) return;
            
            // Try to find pose() method on GuiGraphics
            try {
                poseMethod = GuiGraphics.class.getMethod("pose");
            } catch (NoSuchMethodException e) {
                // pose() doesn't exist - might be 1.21.10+ where it was removed
                PayEveryone.LOGGER.debug("GuiGraphics.pose() not found, trying alternatives");
            }
            
            // If we found pose(), try to get push/pop/translate from the returned PoseStack or Matrix3x2fStack
            if (poseMethod != null && graphics != null) {
                try {
                    Object poseStack = poseMethod.invoke(graphics);
                    if (poseStack != null) {
                        Class<?> poseStackClass = poseStack.getClass();
                        
                        // Try pushPose (PoseStack) then push (Matrix3x2fStack)
                        try {
                            pushPoseMethod = poseStackClass.getMethod("pushPose");
                        } catch (NoSuchMethodException ignored) {
                            try {
                                pushPoseMethod = poseStackClass.getMethod("push");
                            } catch (NoSuchMethodException ignored2) {}
                        }
                        
                        // Try popPose (PoseStack) then pop (Matrix3x2fStack)
                        try {
                            popPoseMethod = poseStackClass.getMethod("popPose");
                        } catch (NoSuchMethodException ignored) {
                            try {
                                popPoseMethod = poseStackClass.getMethod("pop");
                            } catch (NoSuchMethodException ignored2) {}
                        }
                        
                        // Try 3-param translate then 2-param (Matrix3x2fStack is 2D)
                        try {
                            translateMethod = poseStackClass.getMethod("translate", double.class, double.class, double.class);
                        } catch (NoSuchMethodException e) {
                            try {
                                translateMethod = poseStackClass.getMethod("translate", float.class, float.class, float.class);
                            } catch (NoSuchMethodException e2) {
                                try {
                                    translateMethod = poseStackClass.getMethod("translate", float.class, float.class);
                                } catch (NoSuchMethodException ignored) {}
                            }
                        }
                        
                        // Try 3-param scale then 2-param (Matrix3x2fStack is 2D)
                        try {
                            scaleMethod = poseStackClass.getMethod("scale", float.class, float.class, float.class);
                        } catch (NoSuchMethodException e) {
                            try {
                                scaleMethod = poseStackClass.getMethod("scale", float.class, float.class);
                            } catch (NoSuchMethodException ignored) {}
                        }
                    }
                } catch (Exception e) {
                    PayEveryone.LOGGER.debug("Failed to get methods from PoseStack/Matrix3x2fStack", e);
                }
            }
            
            poseMethodsDetected = true;
        }
    }
    
    /**
     * Push pose stack with version compatibility.
     * For 1.21.6+ where pose() returns Matrix3x2fStack (not PoseStack), this uses reflection.
     */
    public static void pushPose(GuiGraphics graphics) {
        if (graphics == null) return;
        
        detectPoseMethods(graphics);
        
        if (poseMethod != null && pushPoseMethod != null) {
            try {
                Object poseStack = poseMethod.invoke(graphics);
                if (poseStack != null) {
                    pushPoseMethod.invoke(poseStack);
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Could not push pose", e);
            }
        }
        // If no pose method exists, just skip - rendering will still work, just without z-ordering
    }
    
    /**
     * Pop pose stack with version compatibility.
     * For 1.21.6+ where pose() returns Matrix3x2fStack (not PoseStack), this uses reflection.
     */
    public static void popPose(GuiGraphics graphics) {
        if (graphics == null) return;
        
        detectPoseMethods(graphics);
        
        if (poseMethod != null && popPoseMethod != null) {
            try {
                Object poseStack = poseMethod.invoke(graphics);
                if (poseStack != null) {
                    popPoseMethod.invoke(poseStack);
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Could not pop pose", e);
            }
        }
    }
    
    /**
     * Translate pose stack with version compatibility.
     * For 1.21.6+ where pose() returns Matrix3x2fStack (not PoseStack), this uses reflection.
     */
    public static void translate(GuiGraphics graphics, double x, double y, double z) {
        if (graphics == null) return;
        
        detectPoseMethods(graphics);
        
        if (poseMethod != null && translateMethod != null) {
            try {
                Object poseStack = poseMethod.invoke(graphics);
                if (poseStack != null) {
                    int paramCount = translateMethod.getParameterCount();
                    Class<?>[] paramTypes = translateMethod.getParameterTypes();
                    
                    if (paramCount == 3) {
                        if (paramTypes[0] == double.class) {
                            translateMethod.invoke(poseStack, x, y, z);
                        } else {
                            translateMethod.invoke(poseStack, (float)x, (float)y, (float)z);
                        }
                    } else if (paramCount == 2) {
                        // Matrix3x2fStack 2D version - z is ignored
                        translateMethod.invoke(poseStack, (float)x, (float)y);
                    }
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Could not translate pose", e);
            }
        }
    }
    
    /**
     * Scale pose stack with version compatibility.
     * For 1.21.6+ where pose() returns Matrix3x2fStack (not PoseStack), this uses reflection.
     */
    public static void scale(GuiGraphics graphics, float x, float y, float z) {
        if (graphics == null) return;
        
        detectPoseMethods(graphics);
        
        if (poseMethod != null && scaleMethod != null) {
            try {
                Object poseStack = poseMethod.invoke(graphics);
                if (poseStack != null) {
                    int paramCount = scaleMethod.getParameterCount();
                    
                    if (paramCount == 3) {
                        scaleMethod.invoke(poseStack, x, y, z);
                    } else if (paramCount == 2) {
                        // Matrix3x2fStack 2D version - z is ignored
                        scaleMethod.invoke(poseStack, x, y);
                    }
                }
            } catch (Exception e) {
                PayEveryone.LOGGER.debug("Could not scale pose", e);
            }
        }
    }
}
