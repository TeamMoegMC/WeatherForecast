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
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server -> client every ServerWorld#tick()
 */
public class WeatherPacket {
    private final int thunderTime;
    private final int rainTime;
    private final int clearTime;
    private final boolean isRaining;
    private final boolean isThunder;

    public WeatherPacket(ServerWorld world) {
        this.thunderTime = world.serverWorldInfo.getThunderTime();
        this.rainTime = world.serverWorldInfo.getRainTime();
        this.clearTime = world.serverWorldInfo.getClearWeatherTime();
        this.isRaining = world.serverWorldInfo.isRaining();
        this.isThunder = world.serverWorldInfo.isThundering();
    }

    public WeatherPacket(PacketBuffer buffer) {
        this.thunderTime = buffer.readVarInt();
        this.rainTime = buffer.readVarInt();
        this.clearTime = buffer.readVarInt();
        this.isRaining = buffer.readBoolean();
        this.isThunder = buffer.readBoolean();
    }

    void encode(PacketBuffer buffer) {
        buffer.writeVarInt(thunderTime);
        buffer.writeVarInt(rainTime);
        buffer.writeVarInt(clearTime);
        buffer.writeBoolean(isRaining);
        buffer.writeBoolean(isThunder);
    }

    void handle(Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            World world = DistExecutor.safeCallWhenOn(Dist.CLIENT, () -> WeatherForecast::getWorld);
            if (world != null) {
                LazyOptional<ITempForecastCapability> cap = TempForecastCapabilityProvider.getCapability(world);
                cap.ifPresent((capability) -> {
                    capability.setRainTime(rainTime);
                    capability.setThunderTime(thunderTime);
                    capability.setClearTime(clearTime);
                    capability.setIsRaining(isRaining);
                    capability.setIsThunder(isThunder);
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}