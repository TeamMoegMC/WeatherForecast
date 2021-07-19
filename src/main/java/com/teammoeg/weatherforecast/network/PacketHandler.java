package com.teammoeg.weatherforecast.network;

import com.teammoeg.weatherforecast.WeatherForecast;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.PacketDistributor;
import net.minecraftforge.fml.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String VERSION = Integer.toString(1);
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(WeatherForecast.rl("network"), () -> VERSION, VERSION::equals, VERSION::equals);

    public static void send(PacketDistributor.PacketTarget target, Object message) {
        CHANNEL.send(target, message);
    }

    public static void sendToServer(Object message) {
        CHANNEL.sendToServer(message);
    }

    public static SimpleChannel get() {
        return CHANNEL;
    }

    @SuppressWarnings("UnusedAssignment")
    public static void register() {
        int id = 0;

        CHANNEL.registerMessage(id++, WeatherPacket.class, WeatherPacket::encode, WeatherPacket::new, WeatherPacket::handle);
        CHANNEL.registerMessage(id++, EveningBroadcastStatusPacket.class, EveningBroadcastStatusPacket::encode, EveningBroadcastStatusPacket::new, EveningBroadcastStatusPacket::handle);
        CHANNEL.registerMessage(id++, MorningBroadcastStatusPacket.class, MorningBroadcastStatusPacket::encode, MorningBroadcastStatusPacket::new, MorningBroadcastStatusPacket::handle);
    }
}