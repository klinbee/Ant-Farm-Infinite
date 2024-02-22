package com.quidvio.two_d_craft;

import net.fabricmc.api.ModInitializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Two_d_craft implements ModInitializer {

    @Override
    public void onInitialize() {
        Registry.register(Registries.CHUNK_GENERATOR, new Identifier("twodcraft", "2d_gen"), ChunkGenerator2D.CODEC);
        //Registry.register(Registries.CHUNK_GENERATOR, new Identifier("twodcraft", "2d_gen"), MyChunkGenerator.CODEC);
    }
}
