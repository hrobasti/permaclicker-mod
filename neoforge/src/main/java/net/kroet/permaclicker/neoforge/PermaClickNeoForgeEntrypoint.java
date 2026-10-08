package net.kroet.permaclicker.neoforge;

import net.kroet.permaclicker.client.PermaClickClient;
import net.kroet.permaclicker.client.PermaClickKeyMappings;
import net.kroet.permaclicker.common.update.ModLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge entrypoint: registers the key mappings and config screen and
 * forwards NeoForge's client events to the shared {@link PermaClickClient}.
 */
// Client-only: on a dedicated server the constructor would load client classes.
@Mod(value = "permaclicker", dist = Dist.CLIENT)
public final class PermaClickNeoForgeEntrypoint {
    public PermaClickNeoForgeEntrypoint(IEventBus modEventBus, ModContainer container) {
        PermaClickClient.init(FMLPaths.GAMEDIR.get(), ModLoader.NEOFORGE, container.getModInfo().getVersion().toString());
        container.registerExtensionPoint(IConfigScreenFactory.class, (modContainer, parent) -> PermaClickClient.createConfigScreen(parent));

        modEventBus.addListener(PermaClickNeoForgeEntrypoint::onRegisterKeyMappings);
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Pre event) -> PermaClickClient.onClientTickStart(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> PermaClickClient.onClientTickEnd(Minecraft.getInstance()));
        NeoForge.EVENT_BUS.addListener((ClientPlayerNetworkEvent.LoggingOut event) -> PermaClickClient.onDisconnect());
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        KeyMapping.Category category = new KeyMapping.Category(PermaClickKeyMappings.CATEGORY_ID);
        event.registerCategory(category);
        KeyMapping toggle = PermaClickKeyMappings.createToggle(category);
        KeyMapping config = PermaClickKeyMappings.createConfig(category);
        event.register(toggle);
        event.register(config);
        PermaClickClient.setKeyMappings(toggle, config);
    }
}
