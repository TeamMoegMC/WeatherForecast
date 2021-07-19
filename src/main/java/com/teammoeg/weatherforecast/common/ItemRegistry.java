package com.teammoeg.weatherforecast.common;

import com.teammoeg.weatherforecast.WeatherForecast;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraftforge.fml.RegistryObject;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

public class ItemRegistry {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, WeatherForecast.MODID);
    public static final RegistryObject<Item> weatherHelmet = ITEMS.register("weather_helmet", () -> new ArmorItem(ModArmorMaterial.WEATHER, EquipmentSlotType.HEAD, (new Item.Properties()).group(WeatherForecast.itemGroup)));
    public static final RegistryObject<Item> weatherRadar = ITEMS.register("weather_radar", () -> new Item((new Item.Properties()).group(WeatherForecast.itemGroup)));
    public static final RegistryObject<Item> temperatureProbe = ITEMS.register("temperature_probe", () -> new Item((new Item.Properties()).group(WeatherForecast.itemGroup)));
}