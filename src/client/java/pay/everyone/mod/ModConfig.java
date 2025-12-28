package pay.everyone.mod;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("pay-everyone.json");
    private static ModConfig instance;
    
    private boolean guiHidden = false;
    
    private ModConfig() {}
    
    public static ModConfig getInstance() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }
    
    public boolean isGuiHidden() {
        return guiHidden;
    }
    
    public void setGuiHidden(boolean hidden) {
        this.guiHidden = hidden;
        save();
    }
    
    private static ModConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                ModConfig config = GSON.fromJson(json, ModConfig.class);
                if (config != null) {
                    return config;
                }
            } catch (IOException e) {
                PayEveryone.LOGGER.error("Failed to load config", e);
            }
        }
        return new ModConfig();
    }
    
    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            PayEveryone.LOGGER.error("Failed to save config", e);
        }
    }
}

