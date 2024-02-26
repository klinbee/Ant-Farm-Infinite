package com.quidvio.ant_farm_inf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.*;
import net.minecraft.util.Identifier;

public class Ant_farm_inf_main implements ModInitializer {

    public static final BorderBlock BORDER_BLOCK  = new BorderBlock(FabricBlockSettings.create().strength(-1.0F, 3600000.8F).collidable(true).dropsNothing().allowsSpawning(Blocks::never).noBlockBreakParticles().pistonBehavior(PistonBehavior.BLOCK));
    @Override
    public void onInitialize() {
        Registry.register(Registries.CHUNK_GENERATOR, new Identifier("ant_farm_inf", "ant_farm_gen"), ChunkGenerator2D.CODEC);
        Registry.register(Registries.BLOCK, new Identifier("ant_farm_inf", "border_block"), BORDER_BLOCK);
    }
}
