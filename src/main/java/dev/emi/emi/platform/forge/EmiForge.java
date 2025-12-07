package dev.emi.emi.platform.forge;

import com.rewindmc.retroemi.PacketReader;
import com.rewindmc.retroemi.RetroEMI;
import dev.emi.emi.EmiPort;
import dev.emi.emi.data.EmiData;
import dev.emi.emi.data.EmiResourceReloadListener;
import dev.emi.emi.mixin.early.accessor.PlayerControllerMPAccessor;
import dev.emi.emi.network.*;
import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.platform.EmiMain;
import dev.emi.emi.runtime.EmiLog;
import dev.emi.emi.runtime.EmiReloadManager;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.server.SPacketCustomPayload;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@Mod(
        modid = "emi",
        guiFactory = "dev.emi.emi.compat.EmiGuiFactory"
)
public class EmiForge {

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        EmiMain.init();
        if (FMLCommonHandler.instance().getSide().isClient()) {
            Client.init();
            MinecraftForge.EVENT_BUS.register(new EmiClientForge());
        }
        EmiNetwork.initServer((player, packet) -> {
            player.connection.sendPacket(toVanilla(packet));
        });
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Mod.EventHandler
    public void postInit(FMLInitializationEvent event) {
        EmiPort.registerReloadListeners((IReloadableResourceManager) Minecraft.getMinecraft().getResourceManager());
        PacketReader.registerServerPacketReader(EmiNetwork.FILL_RECIPE, FillRecipeC2SPacket::new);
        PacketReader.registerServerPacketReader(EmiNetwork.CREATE_ITEM, CreateItemC2SPacket::new);
        PacketReader.registerServerPacketReader(EmiNetwork.CHESS, EmiChessPacket.C2S::new);
    }

//    @SubscribeEvent
//    public void registerCommands(FMLServerStartingEvent event) {
//        event.registerServerCommand(new EmiCommands());
//    }

    @SubscribeEvent
    public void playerConnect(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.player instanceof EntityPlayerMP spe) {
            EmiNetwork.sendToClient(spe, new PingS2CPacket(spe.server.isDedicatedServer() || (spe.server instanceof IntegratedServer integratedServer && integratedServer.getPublic())));
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            RetroEMI.tick();
        }
    }

    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            RetroEMI.tick();
        }
    }

    @SubscribeEvent
    public void onClientConnectedToServer(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        if (!event.isLocal()) {
            EmiReloadManager.reload();
            EmiClient.onServer = true;
        }
    }

    @SubscribeEvent
    public void onClientDisconnection(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        EmiLog.info("Disconnecting from server, EMI data cleared");
        EmiReloadManager.clear();
        EmiClient.onServer = false;
    }

    private static SPacketCustomPayload toVanilla(EmiPacket packet) {
        PacketBuffer buf = new PacketBuffer(Unpooled.buffer());
        packet.write(buf);
        return new SPacketCustomPayload(RetroEMI.compactify(packet.getId()), buf);
    }

    public static final class Client {

        public static void init() {
            EmiClient.init();
            EmiData.init(EmiResourceReloadListener::reload);

            EmiNetwork.initClient(packet -> ((PlayerControllerMPAccessor) Minecraft.getMinecraft().playerController).getConnection().sendPacket(toVanilla(packet)));
            PacketReader.registerClientPacketReader(EmiNetwork.PING, PingS2CPacket::new);
            PacketReader.registerClientPacketReader(EmiNetwork.COMMAND, CommandS2CPacket::new);
            PacketReader.registerClientPacketReader(EmiNetwork.CHESS, EmiChessPacket.S2C::new);
        }
    }
}
