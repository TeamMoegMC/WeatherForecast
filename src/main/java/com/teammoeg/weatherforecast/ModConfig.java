package com.teammoeg.weatherforecast;

import net.minecraftforge.common.ForgeConfigSpec;

public class ModConfig {
    public static class Client {
        public final ForgeConfigSpec.BooleanValue enablesTemperatureOrb;
        public final ForgeConfigSpec.IntValue tempOrbOffsetX;
        public final ForgeConfigSpec.IntValue tempOrbOffsetY;
        public final ForgeConfigSpec.EnumValue<TempOrbPos> tempOrbPosition;

        Client(ForgeConfigSpec.Builder builder) {
            enablesTemperatureOrb = builder
                    .comment("Enables the temperature orb overlay. ")
                    .define("enableTemperatureOrb", true);

            tempOrbPosition = builder
                    .comment("Position of the temperature orb in game screen. ")
                    .defineEnum("renderTempOrbAtCenter", TempOrbPos.MIDDLE);

            tempOrbOffsetX = builder
                    .comment("X Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used. ")
                    .defineInRange("tempOrbOffsetX", 0, -4096, 4096);

            tempOrbOffsetY = builder
                    .comment("Y Offset of the temperature orb. The anchor point is defined by the tempOrbPosition value. Only when you set tempOrbPosition to value other than MIDDLE will this value be used.  ")
                    .defineInRange("tempOrbOffsetY", 0, -4096, 4096);
        }
    }

    public static class Common {

        public final ForgeConfigSpec.BooleanValue enablesTemperatureForecast;
        public final ForgeConfigSpec.BooleanValue requiresTemperatureProbe;
        public final ForgeConfigSpec.BooleanValue giveInitialWeatherTools;

        Common(ForgeConfigSpec.Builder builder) {
            enablesTemperatureForecast = builder
                    .comment("Enables the weather forecast system. ")
                    .define("enablesTemperatureForecast", true);

            requiresTemperatureProbe = builder
                    .comment("Requires the player to have the temperature probe item to show temperature orb overlay. ")
                    .define("requiresTemperatureProbe", true);

            giveInitialWeatherTools = builder
                    .comment("Provides the player initial weather tools on first login. ")
                    .define("giveInitialWeatherTools", false);
        }
    }

    public static final ForgeConfigSpec CLIENT_CONFIG;
    public static final ForgeConfigSpec COMMON_CONFIG;
    public static final Client CLIENT;
    public static final Common COMMON;

    static {
        ForgeConfigSpec.Builder CLIENT_BUILDER = new ForgeConfigSpec.Builder();
        CLIENT = new Client(CLIENT_BUILDER);
        CLIENT_CONFIG = CLIENT_BUILDER.build();
        ForgeConfigSpec.Builder COMMON_BUILDER = new ForgeConfigSpec.Builder();
        COMMON = new Common(COMMON_BUILDER);
        COMMON_CONFIG = COMMON_BUILDER.build();
    }

    public enum TempOrbPos {
        MIDDLE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT;
    }
}
