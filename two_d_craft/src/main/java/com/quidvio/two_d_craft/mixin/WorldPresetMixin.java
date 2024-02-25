package com.quidvio.two_d_craft.mixin;

import com.quidvio.two_d_craft.ChunkGenerator2D;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.gen.WorldPreset;
import net.minecraft.world.gen.WorldPresets;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.ChunkGeneratorSettings;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;

// if you can't mixin here, make sure your access widener is set up correctly!
@Mixin(WorldPresets.Registrar.class)
public abstract class WorldPresetMixin {
    /*
    // defining our registry key. this key provides an Identifier for our preset, that we can use for our lang files and data elements.
    private static final RegistryKey<WorldPreset> TWOD_WORLD = RegistryKey.of(RegistryKeys.WORLD_PRESET, new Identifier("wikiexample", "void_world"));

    protected abstract RegistryEntry<WorldPreset> register(RegistryKey<WorldPreset> key, DimensionOptions dimensionOptions);
    @Shadow protected abstract DimensionOptions createOverworldOptions(ChunkGenerator chunkGenerator);

    @Inject(method = "<init>", at = @At("RETURN"))
    private void addPresets(Registerable presetRegisterable, CallbackInfo ci) {
        // the register() method is shadowed from the target class
        this.register(TWOD_WORLD, this.createOverworldOptions(
                        // a FlatChunkGenerator is the easiest way to get a void world, but you can replace this FlatChunkGenerator constructor with a NoiseChunkGenerator, or your own custom ChunkGenerator.
                        new ChunkGenerator2D((BiomeSource)Registries.BIOME_SOURCE.get(new Identifier("multi_noise")), (RegistryEntry<ChunkGeneratorSettings>) ChunkGeneratorSettings.OVERWORLD)));
    }
     */
}
