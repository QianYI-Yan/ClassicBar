package tfar.classicbar.compat;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.resources.Identifier;

public enum ModCompat {
    vampirism, feathers,legendarysurvivaloverhaul,parcool,thirst,toughasnails;
    public final boolean loaded;

    public Identifier id(String path){
        return Identifier.fromNamespaceAndPath(name(), path);
    }

    ModCompat() {
        loaded = FabricLoader.getInstance().isModLoaded(name());
    }
    
}
