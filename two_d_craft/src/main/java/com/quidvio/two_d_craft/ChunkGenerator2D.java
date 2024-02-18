//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.quidvio.two_d_craft;

import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.noise.NoiseConfig;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Supplier;

public final class ChunkGenerator2D extends ChunkGenerator {

    /* this is a very important field, we will come back to the codec later */
    public static final Codec<ChunkGenerator2D> CODEC = RecordCodecBuilder.create((instance) -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator2D::getBiomeSource), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(ChunkGenerator2D::getSettings)).apply(instance, instance.stable(ChunkGenerator2D::new)));

    static {
        AIR = Blocks.AIR.getDefaultState();
    }

    private static final BlockState AIR;
    public NoiseChunkGenerator defaultGen;
    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final Supplier<AquiferSampler.FluidLevelSampler> fluidLevelSampler;

    /* you can add whatever fields you want to this constructor, as long as they're added to the codec as well */
    public ChunkGenerator2D(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.defaultGen = new NoiseChunkGenerator(biomeSource, settings);
        this.fluidLevelSampler = Suppliers.memoize(() -> {
            return createFluidLevelSampler((ChunkGeneratorSettings)settings.value());
        });
    }

    private static AquiferSampler.FluidLevelSampler createFluidLevelSampler(ChunkGeneratorSettings settings) {
        AquiferSampler.FluidLevel fluidLevel = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
        int i = settings.seaLevel();
        AquiferSampler.FluidLevel fluidLevel2 = new AquiferSampler.FluidLevel(i, settings.defaultFluid());
        AquiferSampler.FluidLevel fluidLevel3 = new AquiferSampler.FluidLevel(DimensionType.MIN_HEIGHT * 2, Blocks.AIR.getDefaultState());
        return (x, y, z) -> {
            return y < Math.min(-54, i) ? fluidLevel : fluidLevel2;
        };
    }

    public CompletableFuture<Chunk> populateBiomes(Executor executor, NoiseConfig noiseConfig, Blender blender, StructureAccessor structureAccessor, Chunk chunk) {
        return defaultGen.populateBiomes(executor,noiseConfig,blender,structureAccessor,chunk);
    }

    public RegistryEntry<ChunkGeneratorSettings> getSettings() {
        return this.settings;
    }


    /* the method that places grass, dirt, and other things on top of the world, as well as handling the bedrock and deepslate layers,
    as well as a few other miscellaneous things. without this method, your world is just a blank stone (or whatever your default block is) canvas (plus any ores, etc) */
    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        defaultGen.buildSurface(region, structures, noiseConfig, chunk);
    }

    /* this method spawns entities in the world */
    @Override
    public void populateEntities(ChunkRegion region) {
        defaultGen.populateEntities(region);
    }

    /* the distance between the highest and lowest points in the world. in vanilla, this is 384 (64+320) */
    @Override
    public int getWorldHeight() {
        return defaultGen.getWorldHeight();
    }

    /* this method builds the shape of the terrain. it places stone everywhere, which will later be overwritten with grass, terracotta, snow, sand, etc
     by the buildSurface method. it also is responsible for putting the water in oceans. it returns a CompletableFuture-- you'll likely want this to be delegated to worker threads. */
    /*
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        if (chunk.getPos().x == 0) {
            try {
                Chunk currentChunk = defaultGen.populateNoise(executor, blender, noiseConfig, structureAccessor, chunk).get();


                Heightmap heightmap = currentChunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
                Heightmap heightmap2 = currentChunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

                BlockPos.Mutable mutable = new BlockPos.Mutable();

                for (int i = 0; i < currentChunk.getHeight(); ++i) {
                    int j = currentChunk.getBottomY() + i;
                    for (int k = 4; k < 16; ++k) {
                        for (int l = 0; l < 16; ++l) {
                            currentChunk.setBlockState(mutable.set(k, j, l), AIR, false);
                            heightmap.trackUpdate(k, j, l, AIR);
                            heightmap2.trackUpdate(k, j, l, AIR);
                        }
                    }
                }
                return CompletableFuture.completedFuture(currentChunk);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }

        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

        BlockPos.Mutable mutable = new BlockPos.Mutable();

        for (int i = 0; i < chunk.getHeight(); ++i) {
            BlockState blockState = Blocks.AIR.getDefaultState();
            if (blockState != null) {
                int j = chunk.getBottomY() + i;
                for (int k = 0; k < 16; ++k) {
                    for (int l = 0; l < 16; ++l) {
                        chunk.setBlockState(mutable.set(k, j, l), blockState, false);
                        heightmap.trackUpdate(k, j, l, blockState);
                        heightmap2.trackUpdate(k, j, l, blockState);
                    }
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

     */
/*
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        if (chunk.getPos().x == 0 && chunk.getPos().z == 0) {
            return defaultGen.populateNoise(executor, blender, noiseConfig, structureAccessor, chunk);
        }
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int i = 0; i < chunk.getHeight(); ++i) {
            int j = chunk.getBottomY() + i;
            for (int k = 0; k < 16; ++k) {
                for (int l = 0; l < 16; ++l) {
                    chunk.setBlockState(mutable.set(k, j, l), AIR, false);
                    heightmap.trackUpdate(k, j, l, AIR);
                    heightmap2.trackUpdate(k, j, l, AIR);
                }
            }
        }
        return CompletableFuture.completedFuture(chunk);
    }

 */

    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        GenerationShapeConfig generationShapeConfig = ((ChunkGeneratorSettings)this.settings.value()).generationShapeConfig().trimHeight(chunk.getHeightLimitView());
        int i = generationShapeConfig.minimumY();
        int j = MathHelper.floorDiv(i, generationShapeConfig.verticalCellBlockCount());
        int k = MathHelper.floorDiv(generationShapeConfig.height(), generationShapeConfig.verticalCellBlockCount());
        if (k <= 0) {
            return CompletableFuture.completedFuture(chunk);
        } else {
            int l = chunk.getSectionIndex(k * generationShapeConfig.verticalCellBlockCount() - 1 + i);
            int m = chunk.getSectionIndex(i);
            Set<ChunkSection> set = Sets.newHashSet();

            for(int n = l; n >= m; --n) {
                ChunkSection chunkSection = chunk.getSection(n);
                chunkSection.lock();
                set.add(chunkSection);
            }

            return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> {
                return this.populateNoise(blender, structureAccessor, noiseConfig, chunk, j, k);
            }), Util.getMainWorkerExecutor()).whenCompleteAsync((chunkx, throwable) -> {
                Iterator var3 = set.iterator();

                while(var3.hasNext()) {
                    ChunkSection chunkSection = (ChunkSection)var3.next();
                    chunkSection.unlock();
                }

            }, executor);
        }
    }

    private ChunkNoiseSampler createChunkNoiseSampler(Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig) {
        return ChunkNoiseSampler.create(chunk, noiseConfig, StructureWeightSampler.createStructureWeightSampler(world, chunk.getPos()), (ChunkGeneratorSettings)this.settings.value(), (AquiferSampler.FluidLevelSampler)this.fluidLevelSampler.get(), blender);
    }

    private Chunk populateNoise(Blender blender, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk, int minimumCellY, int cellHeight) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler((chunkx) -> {
            return this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig);
        });
        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);
        ChunkPos chunkPos = chunk.getPos();
        int i = chunkPos.getStartX();
        int j = chunkPos.getStartZ();
        AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
        chunkNoiseSampler.sampleStartDensity();
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        int k = chunkNoiseSampler.getHorizontalCellBlockCount();
        int l = chunkNoiseSampler.getVerticalCellBlockCount();
        int m = 16 / k;
        int n = 16 / k;

        for(int o = 0; o < m; ++o) {
            chunkNoiseSampler.sampleEndDensity(o);

            for(int p = 0; p < n; ++p) {
                int q = chunk.countVerticalSections() - 1;
                ChunkSection chunkSection = chunk.getSection(q);

                for(int r = cellHeight - 1; r >= 0; --r) {
                    chunkNoiseSampler.onSampledCellCorners(r, p);

                    for(int s = l - 1; s >= 0; --s) {
                        int t = (minimumCellY + r) * l + s;
                        int u = t & 15;
                        int v = chunk.getSectionIndex(t);
                        if (q != v) {
                            q = v;
                            chunkSection = chunk.getSection(v);
                        }

                        double d = (double)s / (double)l;
                        chunkNoiseSampler.interpolateY(t, d);

                        for(int w = 0; w < k; ++w) {
                            int x = i + o * k + w;
                            int y = x & 15;
                            double e = (double)w / (double)k;
                            chunkNoiseSampler.interpolateX(x, e);

                            for(int z = 0; z < k; ++z) {
                                int aa = j + p * k + z;
                                int ab = aa & 15;
                                double f = (double)z / (double)k;
                                chunkNoiseSampler.interpolateZ(aa, f);
                                BlockState blockState = chunkNoiseSampler.sampleBlockState();
                                if (blockState == null) {
                                    blockState = ((ChunkGeneratorSettings)this.settings.value()).defaultBlock();
                                }

                                blockState = this.getBlockState(chunkNoiseSampler, x, t, aa, blockState);
                                if (blockState != AIR && !SharedConstants.isOutsideGenerationArea(chunk.getPos())) {
                                    chunkSection.setBlockState(y, u, ab, blockState, false);
                                    heightmap.trackUpdate(y, t, ab, blockState);
                                    heightmap2.trackUpdate(y, t, ab, blockState);
                                    if (aquiferSampler.needsFluidTick() && !blockState.getFluidState().isEmpty()) {
                                        mutable.set(x, t, aa);
                                        chunk.markBlockForPostProcessing(mutable);
                                    }
                                }
                            }
                        }
                    }
                }
            }

            chunkNoiseSampler.swapBuffers();
        }

        chunkNoiseSampler.stopInterpolation();
        return chunk;
    }

    private BlockState getBlockState(ChunkNoiseSampler chunkNoiseSampler, int x, int y, int z, BlockState state) {
        return state;
    }


    @Override
    public int getSeaLevel() {
        return 0;
    }

    /* the lowest value that blocks can be placed in the world. in a vanilla world, this is -64. */
    @Override
    public int getMinimumY() {
        return defaultGen.getMinimumY();
    }

    /* this method returns the height of the terrain at a given coordinate. it's used for structure generation */
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return defaultGen.getHeight(x, z, heightmap, world, noiseConfig);
    }

    /* this method returns a "core sample" of the world at a given coordinate. it's used for structure generation */
    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        return defaultGen.getColumnSample(x, z, world, noiseConfig);
    }

    /* this method adds text to the f3 menu. for NoiseChunkGenerator, it's the NoiseRouter line */
    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        defaultGen.getDebugHudText(text, noiseConfig, pos);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        defaultGen.carve(chunkRegion, seed, noiseConfig, biomeAccess, structureAccessor, chunk, carverStep);
    }
}
