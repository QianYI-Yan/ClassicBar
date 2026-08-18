package tfar.classicbar.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.ResourceLocation;

public enum ModCompat {
    vampirism, feathers,legendarysurvivaloverhaul,parcool,thirst,toughasnails;
    public final boolean loaded;

    public ResourceLocation id(String path){
        return new ResourceLocation(name(), path);
    }

    ModCompat() {
        loaded = FabricLoader.getInstance().isModLoaded(name());
    }
    
}
