package dev.emi.emi.network;

import dev.emi.emi.platform.EmiClient;
import dev.emi.emi.runtime.EmiReloadManager;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

public class PingS2CPacket implements EmiPacket {
    private int isServer;

    public PingS2CPacket(boolean isServer) {
        this.isServer = isServer ? 1 : 0;
    }

    public PingS2CPacket(PacketBuffer buf) {
        isServer = buf.readByte();
    }

    @Override
    public void write(PacketBuffer buf) {
        buf.writeByte(isServer);
    }

    @Override
    public void apply(EntityPlayer player) {
        EmiClient.onServer = isServer == 1;
        EmiReloadManager.reload();
    }

    @Override
    public ResourceLocation getId() {
        return EmiNetwork.PING;
    }
}
