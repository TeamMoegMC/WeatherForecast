/*
 * Work under Copyright. Licensed under the EUPL.
 * See the project README.md and LICENSE.txt for more information.
 */

package com.teammoeg.weatherforecast.network;

import com.teammoeg.weatherforecast.WeatherForecast;
import com.teammoeg.weatherforecast.capability.ITempForecastCapability;
import com.teammoeg.weatherforecast.capability.TempForecastCapabilityProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server -> client on broadcast
 */
public class EveningBroadcastStatusPacket {

    private final boolean eveningStatus;

    public EveningBroadcastStatusPacket(boolean newStatus) {
        this.eveningStatus = newStatus;
    }

    public EveningBroadcastStatusPacket(PacketBuffer buffer) {
        this.eveningStatus = buffer.readBoolean();
    }

    void encode(PacketBuffer buffer) {
        buffer.writeBoolean(eveningStatus);
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> WeatherForecast::getWorld);
            if (world != null) {
                LazyOptional<ITempForecastCapability> cap = TempForecastCapabilityProvider.getCapability(world);
                cap.ifPresent((capability) -> {
                    capability.setEveningForecastStatus(eveningStatus);
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}