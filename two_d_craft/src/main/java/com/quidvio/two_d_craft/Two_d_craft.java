package com.quidvio.two_d_craft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class Two_d_craft implements ModInitializer {

    public static final BorderBlock BORDER_BLOCK  = new BorderBlock(FabricBlockSettings.create().strength(-1.0F, 3600000.8F).collidable(true).dropsNothing().allowsSpawning(Blocks::never).noBlockBreakParticles().pistonBehavior(PistonBehavior.BLOCK));
    @Override
    public void onInitialize() {
        Registry.register(Registries.CHUNK_GENERATOR, new Identifier("twodcraft", "2d_gen"), ChunkGenerator2D.CODEC);
        Registry.register(Registries.BLOCK, new Identifier("twodcraft", "border_block"), BORDER_BLOCK);
    }
}
