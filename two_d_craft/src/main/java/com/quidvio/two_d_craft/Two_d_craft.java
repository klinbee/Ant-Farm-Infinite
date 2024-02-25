package com.quidvio.two_d_craft;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.registry.DynamicRegistries;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryElementCodec;
import net.minecraft.structure.StructureSet;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.chunk.placement.StructurePlacement;

import java.util.function.BiFunction;

public class Two_d_craft implements ModInitializer {

    public static final BorderBlock BORDER_BLOCK  = new BorderBlock(FabricBlockSettings.create().strength(-1.0F, 3600000.8F).collidable(true).dropsNothing().allowsSpawning(Blocks::never).noBlockBreakParticles().pistonBehavior(PistonBehavior.BLOCK));
    @Override
    public void onInitialize() {
        Registry.register(Registries.CHUNK_GENERATOR, new Identifier("twodcraft", "2d_gen"), ChunkGenerator2D.CODEC);
        Registry.register(Registries.BLOCK, new Identifier("twodcraft", "border_block"), BORDER_BLOCK);
    }
}
