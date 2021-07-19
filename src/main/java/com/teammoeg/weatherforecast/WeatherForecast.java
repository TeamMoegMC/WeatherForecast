package com.teammoeg.weatherforecast;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.teammoeg.weatherforecast.capability.ITempForecastCapability;
import com.teammoeg.weatherforecast.capability.TempForecastCapability;
import com.teammoeg.weatherforecast.capability.TempForecastCapabilityProvider;
import com.teammoeg.weatherforecast.common.ItemRegistry;
import com.teammoeg.weatherforecast.network.PacketHandler;
import com.teammoeg.weatherforecast.network.WeatherPacket;
import com.teammoeg.weatherforecast.util.UV4i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.AbstractGui;
import net.minecraft.client.gui.IngameGui;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.PacketDistributor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.List;

@Mod(WeatherForecast.MODID)
public class WeatherForecast {
    public static final String MODID = "weatherforecast";
    private static final Logger LOGGER = LogManager.getLogger();

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MODID, path);
    }

    public static World getWorld() {
        return Minecraft.getInstance().world;
    }

    public static ItemGroup itemGroup = new ItemGroup(MODID) {
        @Override
        @Nonnull
        public ItemStack createIcon() {
            return new ItemStack(ItemRegistry.weatherRadar.get());
        }
    };

    public WeatherForecast() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::doClientStuff);
        MinecraftForge.EVENT_BUS.register(this);
        ItemRegistry.ITEMS.register(FMLJavaModLoadingContext.get().getModEventBus());
        PacketHandler.register();
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.CLIENT, ModConfig.CLIENT_CONFIG);
        ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.COMMON, ModConfig.COMMON_CONFIG);
    }

    private void setup(final FMLCommonSetupEvent event) {
        TempForecastCapabilityProvider.setup();
    }

    private void doClientStuff(final FMLClientSetupEvent event) {

    }

    @Mod.EventBusSubscriber(modid = WeatherForecast.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class CommonForgeEvents {
        @SubscribeEvent
        public static void onServerTick(TickEvent.WorldTickEvent event) {
            if (!event.world.isRemote) {
                PacketHandler.send(PacketDistributor.ALL.noArg(), new WeatherPacket((ServerWorld) event.world));
            }
        }

        @SubscribeEvent
        public static void onAttachCapabilitiesWorld(AttachCapabilitiesEvent<World> event) {
            if (event.getObject() != null) {
                event.addCapability(TempForecastCapabilityProvider.KEY, new TempForecastCapabilityProvider());
            }
        }

        @SubscribeEvent
        public static void addItemToolTip(ItemTooltipEvent event) {
            ItemStack stack = event.getItemStack();
            if (stack.getItem() == ItemRegistry.temperatureProbe.get()) {
                event.getToolTip().add(new TranslationTextComponent("tooltip.weatherforecast.temperature_probe").mergeStyle(TextFormatting.GRAY));
            }
            if (stack.getItem() == ItemRegistry.weatherRadar.get()) {
                event.getToolTip().add(new TranslationTextComponent("tooltip.weatherforecast.weather_radar").mergeStyle(TextFormatting.GRAY));
            }
            if (stack.getItem() == ItemRegistry.weatherHelmet.get()) {
                event.getToolTip().add(new TranslationTextComponent("tooltip.weatherforecast.weather_helmet").mergeStyle(TextFormatting.GRAY));
            }
        }
    }

    @Mod.EventBusSubscriber(modid = WeatherForecast.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onRenderGameOverlayText(RenderGameOverlayEvent.Text event) {
            Minecraft mc = Minecraft.getInstance();
            List<String> list = event.getRight();
            if (mc.world != null && mc.gameSettings.showDebugInfo) {
                mc.world.getCapability(TempForecastCapabilityProvider.CAPABILITY).ifPresent((capability -> {
                    int clearTime = capability.getClearTime();
                    int rainTime = capability.getRainTime();
                    int thunderTime = capability.getThunderTime();
                    list.add("[WF] Ticks until clear: " + clearTime);
                    list.add("[WF] Ticks until rain: " + rainTime);
                    list.add("[WF] Ticks until thunder: " + thunderTime);
                }));
            }
        }

        @SubscribeEvent
        public static void renderForecastResult(RenderGameOverlayEvent event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                boolean flag0 = ModConfig.COMMON.enablesTemperatureForecast.get();
                boolean flag1 = !ModConfig.COMMON.requiresWeatherRadar.get() || mc.player.inventory.hasItemStack(new ItemStack(ItemRegistry.weatherRadar.get()));
                boolean flag2 = !ModConfig.COMMON.requiresWeatherHelmet.get() || mc.player.inventory.armorItemInSlot(3).isItemEqualIgnoreDurability(new ItemStack(ItemRegistry.weatherHelmet.get()));
                boolean configAllows = flag0 && flag1 && flag2;
                if (configAllows && Minecraft.isGuiEnabled() && mc.world != null && !mc.gameSettings.showDebugInfo) {
                    ITempForecastCapability capability = mc.world.getCapability(TempForecastCapabilityProvider.CAPABILITY).orElse(new TempForecastCapability(0, 0, 0, false, false));
                    int clearTime = capability.getClearTime();
                    int rainTime = capability.getRainTime();
                    int thunderTime = capability.getThunderTime();
                    boolean isRaining = capability.getIsRaining();
                    boolean isThunder = capability.getIsThunder();
                    // Sleep time broadcast
                    if (mc.world.getDayTime() % 24000 == 12542 && !capability.getEveningForecastStatus()) {
                        if (!isRaining && !isThunder) {
                            // thunder will happen before 6AM tomorrow (tonight)
                            if (thunderTime <= 24000 - 12542) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.thunder_approach_tonight"));
                            }
                            // thunder will happen after 6AM tomorrow (tomorrow)
                            else if (thunderTime <= 24000) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.thunder_approach_tomorrow"));
                            }
                            // rain has lower priority than thunder
                            else if (rainTime <= 24000 - 12542) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.rain_approach_tonight"));
                            } else if (rainTime <= 24000) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.rain_approach_tomorrow"));
                            } else {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.no_rain_or_thunder_tomorrow"));
                            }
                        } else {
                            if (isThunder) {
                                // sky will be clear tonight
                                if (thunderTime <= 24000 - 12542) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_tonight"));
                                    // sky will be clear tomorrow
                                } else if (thunderTime <= 24000) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_tomorrow"));
                                } else {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.continue_rain_or_thunder_tomorrow"));
                                }
                            } else {
                                // sky will be clear tonight
                                if (rainTime <= 24000 - 12542) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_tonight"));
                                    // sky will be clear tomorrow
                                } else if (rainTime <= 24000) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_tomorrow"));
                                } else {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.continue_rain_or_thunder_tomorrow"));
                                }
                            }
                        }
                        capability.setEveningForecastStatus(true);
                        capability.setMorningForecastStatus(false);
                    }

                    // Morning broadcast
                    if (mc.world.getDayTime() % 24000 == 40 && !capability.getMorningForecastStatus()) {
                        if (!isRaining && !isThunder) {
                            // 6AM - Sleeptime
                            if (thunderTime <= 12542) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.thunder_approach_today"));
                            }
                            // Sleeptime - 6AM tomorrow
                            else if (thunderTime <= 24000) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.thunder_approach_evening"));
                            }
                            // rain has lower priority than thunder
                            else if (rainTime <= 12542) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.rain_approach_today"));
                            } else if (rainTime <= 24000) {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.rain_approach_evening"));
                            } else {
                                mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.no_rain_or_thunder_today"));
                            }
                        } else {
                            if (isThunder) {
                                // Clear 6AM - Sleeptime
                                if (thunderTime <= 12542) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_today"));
                                    // Clear Sleeptime - 6AM tomorrow
                                } else if (thunderTime <= 24000) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_evening"));
                                } else {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.continue_rain_or_thunder_today"));
                                }
                            } else {
                                // Clear 6AM - Sleeptime
                                if (rainTime <= 12542) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_today"));
                                    // Clear Sleeptime - 6AM tomorrow
                                } else if (rainTime <= 24000) {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.clear_sky_evening"));
                                } else {
                                    mc.ingameGUI.getChatGUI().printChatMessage(new TranslationTextComponent("weatherforecast.message.continue_rain_or_thunder_today"));
                                }
                            }
                        }
                        capability.setMorningForecastStatus(true);
                        capability.setEveningForecastStatus(false);
                    }
                }
            }

        }

        @SubscribeEvent
        public static void renderTemperatureOrb(RenderGameOverlayEvent event) {
            Minecraft mc = Minecraft.getInstance();
            mc.getProfiler().startSection("weatherforecast_temperature");
            if (ModConfig.CLIENT.enablesTemperatureOrb.get() && Minecraft.isGuiEnabled() && mc.world != null && !mc.gameSettings.showDebugInfo && mc.playerController != null && mc.playerController.gameIsSurvivalOrAdventure() && mc.getRenderViewEntity() != null) {
                if (mc.player != null && (!ModConfig.COMMON.requiresTemperatureProbe.get() || mc.player.inventory.hasItemStack(new ItemStack(ItemRegistry.temperatureProbe.get())))) {
                    BlockPos pos = new BlockPos(mc.getRenderViewEntity().getPosX(), mc.getRenderViewEntity().getBoundingBox().minY, mc.getRenderViewEntity().getPosZ());
                    double temperature = MathHelper.clamp(mc.world.getBiome(pos).getTemperature(pos), -0.5, 2.0);

                    // PART 1: BIOME FACTOR (HEIGHT-DEPENDENT)
                    // range: -0.5 (snowy taiga) to 2.0 (desert)
                    // plain is 0.8, we assume spring is the only season here, and take 25 degree celsius here
                    // ocean is 0.5, we take 0 degree celsius here
                    // desert is 2.0, we take 40 degree celsius here
                    // normal snowy biomes are 0.0, we assume -20 degree celsius
                    // taiga biomes (snowy) are -0.5, we assume -40 degree celsius
                    // weighted range: -40 to +40 celsius
                    double weightedTemp;
                    if (temperature >= 0.8) {
                        weightedTemp = 25 + (temperature - 0.8) * 12.5;
                    } else if (temperature >= 0.5) {
                        weightedTemp = 0 + (temperature - 0.5) * 83.3;
                    } else {
                        weightedTemp = -40 + (temperature + 0.5) * 40.0;
                    }

                    // PART 2: WEATHER FACTOR
                    ITempForecastCapability cap = mc.world.getCapability(TempForecastCapabilityProvider.CAPABILITY).orElse(new TempForecastCapability(0, 0, 0, false, false));
                    if (cap.getIsRaining()) {
                        weightedTemp -= 10;
                    }
                    if (cap.getIsThunder()) {
                        weightedTemp -= 20;
                    }

                    // PART 3: TIME FACTOR
                    long dayTime = mc.world.getDayTime() % 24000;
                    // Our previous weighted temperature is 2PM peak temperature, which is 8000 ticks
                    // 6AM to 2PM is increasing, 2PM to 6AM is decreasing
                    if ((dayTime >= 0 && dayTime <= 8000)) {
                        weightedTemp = weightedTemp + (dayTime - 8000) * 10 / 8000f;
                    } else {
                        weightedTemp = weightedTemp - (dayTime - 8000) * 10 / 16000f;
                    }

                    // RENDER CONFIGURATION
                    int w = event.getWindow().getScaledWidth();
                    int h = event.getWindow().getScaledHeight();
                    int offsetX = 0;
                    int offsetY = 0;
                    if (ModConfig.CLIENT.tempOrbPosition.get() == ModConfig.TempOrbPos.MIDDLE) {
                        offsetX = w / 2 - 18;
                        offsetY = h - 84;
                    }
                    else if (ModConfig.CLIENT.tempOrbPosition.get() == ModConfig.TempOrbPos.TOP_LEFT) {
                        offsetX = ModConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = ModConfig.CLIENT.tempOrbOffsetY.get();
                    }
                    else if (ModConfig.CLIENT.tempOrbPosition.get() == ModConfig.TempOrbPos.TOP_RIGHT) {
                        offsetX = w - 36 + ModConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = ModConfig.CLIENT.tempOrbOffsetY.get();
                    }
                    else if (ModConfig.CLIENT.tempOrbPosition.get() == ModConfig.TempOrbPos.BOTTOM_LEFT) {
                        offsetX = ModConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = h - 36 + ModConfig.CLIENT.tempOrbOffsetY.get();
                    }
                    else if (ModConfig.CLIENT.tempOrbPosition.get() == ModConfig.TempOrbPos.BOTTOM_RIGHT) {
                        offsetX = w - 36 + ModConfig.CLIENT.tempOrbOffsetX.get();
                        offsetY = h - 36 + ModConfig.CLIENT.tempOrbOffsetY.get();
                    }

                    // RENDER ORB
                    renderTemp(event.getMatrixStack(), mc, weightedTemp, offsetX, offsetY,true);
                }
            }

            mc.getTextureManager().bindTexture(AbstractGui.GUI_ICONS_LOCATION);
            mc.getProfiler().endSection();

            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
            RenderSystem.disableAlphaTest();
        }

        private static void renderTemp(MatrixStack stack, Minecraft mc, double temp, int offsetX, int offsetY, boolean celsius) {
            UV4i unitUV = celsius ? new UV4i(0, 25, 13, 34) : new UV4i(13, 25, 26, 34);
            UV4i signUV = temp >= 0 ? new UV4i(61, 17, 68, 24) : new UV4i(68, 17, 75, 24);
            double abs = Math.abs(temp);
            BigDecimal bigDecimal = new BigDecimal(String.valueOf(abs));
            bigDecimal.round(new MathContext(1));
            int integer = bigDecimal.intValue();
            int decimal = (int) (bigDecimal.subtract(new BigDecimal(integer)).doubleValue() * 10);

            ResourceLocation digits = new ResourceLocation(WeatherForecast.MODID, "textures/gui/temperature_orb/digits.png");
            ResourceLocation moderate = new ResourceLocation(WeatherForecast.MODID, "textures/gui/temperature_orb/moderate.png");
            ResourceLocation chilly = new ResourceLocation(WeatherForecast.MODID, "textures/gui/temperature_orb/chilly.png");
            ResourceLocation cold = new ResourceLocation(WeatherForecast.MODID, "textures/gui/temperature_orb/cold.png");
            ResourceLocation frigid = new ResourceLocation(WeatherForecast.MODID, "textures/gui/temperature_orb/frigid.png");
            ResourceLocation hadean = new ResourceLocation(WeatherForecast.MODID, "textures/gui/temperature_orb/hadean.png");

            // draw orb
            if (temp > 0) {
                mc.getTextureManager().bindTexture(moderate);
            } else if (temp > -20) {
                mc.getTextureManager().bindTexture(chilly);
            } else if (temp > -40) {
                mc.getTextureManager().bindTexture(cold);
            } else if (temp > -80) {
                mc.getTextureManager().bindTexture(frigid);
            } else {
                mc.getTextureManager().bindTexture(hadean);
            }
            IngameGui.blit(stack, offsetX + 0, offsetY + 0, 0, 0, 36, 36, 36, 36);

            // draw temperature
            mc.getTextureManager().bindTexture(digits);
            // sign and unit
            IngameGui.blit(stack, offsetX + 1, offsetY + 12, signUV.x, signUV.y, signUV.w, signUV.h, 100, 34);
            IngameGui.blit(stack, offsetX + 11, offsetY + 24, unitUV.x, unitUV.y, unitUV.w, unitUV.h, 100, 34);
            // digits
            ArrayList<UV4i> uv4is = getIntegerDigitUVs(integer);
            UV4i decUV = getDecDigitUV(decimal);
            if (uv4is.size() == 1) {
                UV4i uv1 = uv4is.get(0);
                IngameGui.blit(stack, offsetX + 13, offsetY + 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
                IngameGui.blit(stack, offsetX + 25, offsetY + 16, decUV.x, decUV.y, decUV.w, decUV.h, 100, 34);
            } else if (uv4is.size() == 2) {
                UV4i uv1 = uv4is.get(0), uv2 = uv4is.get(1);
                IngameGui.blit(stack, offsetX + 8, offsetY + 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
                IngameGui.blit(stack, offsetX + 18, offsetY + 7, uv2.x, uv2.y, uv2.w, uv2.h, 100, 34);
                IngameGui.blit(stack, offsetX + 28, offsetY + 16, decUV.x, decUV.y, decUV.w, decUV.h, 100, 34);
            } else if (uv4is.size() == 3) {
                UV4i uv1 = uv4is.get(0), uv2 = uv4is.get(1), uv3 = uv4is.get(2);
                IngameGui.blit(stack, offsetX + 7, offsetY + 7, uv1.x, uv1.y, uv1.w, uv1.h, 100, 34);
                IngameGui.blit(stack, offsetX + 14, offsetY + 7, uv2.x, uv2.y, uv2.w, uv2.h, 100, 34);
                IngameGui.blit(stack, offsetX + 24, offsetY + 7, uv3.x, uv3.y, uv3.w, uv3.h, 100, 34);
            }
        }

        private static ArrayList<UV4i> getIntegerDigitUVs(int digit) {
            ArrayList<UV4i> rtn = new ArrayList<>();
            UV4i v1, v2, v3;
            if (digit / 10 == 0) { // len = 1
                int firstDigit = digit; if (firstDigit == 0) firstDigit += 10;
                v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
                rtn.add(v1);
            } else if (digit / 10 < 10) { // len = 2
                int firstDigit = digit / 10; if (firstDigit == 0) firstDigit += 10;
                int secondDigit = digit % 10; if (secondDigit == 0) secondDigit += 10;
                v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
                v2 = new UV4i(10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
                rtn.add(v1);
                rtn.add(v2);
            } else { // len = 3
                int thirdDigit = digit % 10; if (thirdDigit == 0) thirdDigit += 10;
                int secondDigit = digit / 10; if (secondDigit == 0) secondDigit += 10;
                int firstDigit = digit / 100; if (firstDigit == 0) firstDigit += 10;
                v1 = new UV4i(10 * (firstDigit - 1), 0, 10 * firstDigit, 17);
                v2 = new UV4i(10 * (secondDigit - 1), 0, 10 * secondDigit, 17);
                v3 = new UV4i(10 * (thirdDigit - 1), 0, 10 * thirdDigit, 17);
                rtn.add(v1);
                rtn.add(v2);
                rtn.add(v3);
            }
            return rtn;
        }

        private static UV4i getDecDigitUV(int dec) {
            return new UV4i(6 * (dec - 1), 17, 6 * dec, 25);
        }

    }
}
