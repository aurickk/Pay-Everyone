package pay.everyone.mod;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PayEveryone implements ModInitializer {
	public static final String MOD_ID = "pay-everyone";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Pay Everyone mod initialized");
	}
}