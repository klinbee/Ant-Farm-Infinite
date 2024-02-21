//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.quidvio.two_d_craft;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.datafixers.kinds.App;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap;
import net.minecraft.world.SpawnHelper;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.chunk.BelowZeroRetrogen;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ProtoChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Shadow;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ChunkGenerator2D extends ChunkGenerator {

    /* this is a very important field, we will come back to the codec later */
    public static final Codec<ChunkGenerator2D> CODEC = RecordCodecBuilder.create((instance) -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator2D::getBiomeSource), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(ChunkGenerator2D::getSettings)).apply(instance, instance.stable(ChunkGenerator2D::new)));
    //public static final Codec<NoiseChunkGenerator> CODEC = RecordCodecBuilder.create((instance) -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(NoiseChunkGenerator::getBiomeSource), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(NoiseChunkGenerator::getSettings)).apply(instance, instance.stable(NoiseChunkGenerator::new)));


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
        //this.defaultGen = new NoiseChunkGenerator(biomeSource, settings);
        this.fluidLevelSampler = Suppliers.memoize(() -> {
            return createFluidLevelSampler((ChunkGeneratorSettings) settings.value());
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
        return CompletableFuture.supplyAsync(Util.debugSupplier("init_biomes", () -> {
            this.populateBiomes(blender, noiseConfig, structureAccessor, chunk);
            return chunk;
        }), Util.getMainWorkerExecutor());
    }

    private void populateBiomes(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler((chunkx) -> {
            return this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig);
        });
        BiomeSupplier biomeSupplier = BelowZeroRetrogen.getBiomeSupplier(blender.getBiomeSupplier(this.biomeSource), chunk);
        chunk.populateBiomes(biomeSupplier, chunkNoiseSampler.createMultiNoiseSampler(noiseConfig.getNoiseRouter(), ((ChunkGeneratorSettings) this.settings.value()).spawnTarget()));
    }

    private ChunkNoiseSampler createChunkNoiseSampler(Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig) {
        return ChunkNoiseSampler.create(chunk, noiseConfig, StructureWeightSampler.createStructureWeightSampler(world, chunk.getPos()), (ChunkGeneratorSettings) this.settings.value(), (AquiferSampler.FluidLevelSampler) this.fluidLevelSampler.get(), blender);
    }

    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    public RegistryEntry<ChunkGeneratorSettings> getSettings() {
        return this.settings;
    }

    public boolean matchesSettings(RegistryKey<ChunkGeneratorSettings> settings) {
        return this.settings.matchesKey(settings);
    }

    /* this method returns the height of the terrain at a given coordinate. it's used for structure generation */
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return this.sampleHeightmap(world, noiseConfig, x, z, (MutableObject) null, heightmap.getBlockPredicate()).orElse(world.getBottomY());
    }

    /* this method returns a "core sample" of the world at a given coordinate. it's used for structure generation */
    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        MutableObject<VerticalBlockSample> mutableObject = new MutableObject();
        this.sampleHeightmap(world, noiseConfig, x, z, mutableObject, (Predicate) null);
        return (VerticalBlockSample) mutableObject.getValue();
    }

    /* this method adds text to the f3 menu. for NoiseChunkGenerator, it's the NoiseRouter line */
    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        NoiseRouter noiseRouter = noiseConfig.getNoiseRouter();
        DensityFunction.UnblendedNoisePos unblendedNoisePos = new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ());
        double d = noiseRouter.ridges().sample(unblendedNoisePos);
        String var10001 = decimalFormat.format(noiseRouter.temperature().sample(unblendedNoisePos));
        text.add("NoiseRouter T: " + var10001 + " V: " + decimalFormat.format(noiseRouter.vegetation().sample(unblendedNoisePos)) + " C: " + decimalFormat.format(noiseRouter.continents().sample(unblendedNoisePos)) + " E: " + decimalFormat.format(noiseRouter.erosion().sample(unblendedNoisePos)) + " D: " + decimalFormat.format(noiseRouter.depth().sample(unblendedNoisePos)) + " W: " + decimalFormat.format(d) + " PV: " + decimalFormat.format((double) DensityFunctions.getPeaksValleysNoise((float) d)) + " AS: " + decimalFormat.format(noiseRouter.initialDensityWithoutJaggedness().sample(unblendedNoisePos)) + " N: " + decimalFormat.format(noiseRouter.finalDensity().sample(unblendedNoisePos)));
    }

    private OptionalInt sampleHeightmap(HeightLimitView world, NoiseConfig noiseConfig, int x, int z, @Nullable MutableObject<VerticalBlockSample> columnSample, @Nullable Predicate<BlockState> stopPredicate) {
        GenerationShapeConfig generationShapeConfig = ((ChunkGeneratorSettings) this.settings.value()).generationShapeConfig().trimHeight(world);
        int i = generationShapeConfig.verticalCellBlockCount();
        int j = generationShapeConfig.minimumY();
        int k = MathHelper.floorDiv(j, i);
        int l = MathHelper.floorDiv(generationShapeConfig.height(), i);
        if (l <= 0) {
            return OptionalInt.empty();
        } else {
            BlockState[] blockStates;
            if (columnSample == null) {
                blockStates = null;
            } else {
                blockStates = new BlockState[generationShapeConfig.height()];
                columnSample.setValue(new VerticalBlockSample(j, blockStates));
            }

            int m = generationShapeConfig.horizontalCellBlockCount();
            int n = Math.floorDiv(x, m);
            int o = Math.floorDiv(z, m);
            int p = Math.floorMod(x, m);
            int q = Math.floorMod(z, m);
            int r = n * m;
            int s = o * m;
            double d = (double) p / (double) m;
            double e = (double) q / (double) m;
            ChunkNoiseSampler chunkNoiseSampler = new ChunkNoiseSampler(1, noiseConfig, r, s, generationShapeConfig, DensityFunctionTypes.Beardifier.INSTANCE, (ChunkGeneratorSettings) this.settings.value(), (AquiferSampler.FluidLevelSampler) this.fluidLevelSampler.get(), Blender.getNoBlending());
            chunkNoiseSampler.sampleStartDensity();
            chunkNoiseSampler.sampleEndDensity(0);

            for (int t = l - 1; t >= 0; --t) {
                chunkNoiseSampler.onSampledCellCorners(t, 0);

                for (int u = i - 1; u >= 0; --u) {
                    int v = (k + t) * i + u;
                    double f = (double) u / (double) i;
                    chunkNoiseSampler.interpolateY(v, f);
                    chunkNoiseSampler.interpolateX(x, d);
                    chunkNoiseSampler.interpolateZ(z, e);
                    BlockState blockState = chunkNoiseSampler.sampleBlockState();
                    BlockState blockState2 = blockState == null ? ((ChunkGeneratorSettings) this.settings.value()).defaultBlock() : blockState;
                    if (blockStates != null) {
                        int w = t * i + u;
                        blockStates[w] = blockState2;
                    }

                    if (stopPredicate != null && stopPredicate.test(blockState2)) {
                        chunkNoiseSampler.stopInterpolation();
                        return OptionalInt.of(v + 1);
                    }
                }
            }

            chunkNoiseSampler.stopInterpolation();
            return OptionalInt.empty();
        }
    }


    /* the method that places grass, dirt, and other things on top of the world, as well as handling the bedrock and deepslate layers,
    as well as a few other miscellaneous things. without this method, your world is just a blank stone (or whatever your default block is) canvas (plus any ores, etc) */
    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        if (!SharedConstants.isOutsideGenerationArea(chunk.getPos())) {
            HeightContext heightContext = new HeightContext(this, region);
            this.buildSurface(chunk, heightContext, noiseConfig, structures, region.getBiomeAccess(), region.getRegistryManager().get(RegistryKeys.BIOME), Blender.getBlender(region));
        }
    }

    @VisibleForTesting
    public void buildSurface(Chunk chunk, HeightContext heightContext, NoiseConfig noiseConfig, StructureAccessor structureAccessor, BiomeAccess biomeAccess, Registry<Biome> biomeRegistry, Blender blender) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler((chunkx) -> {
            return this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig);
        });
        ChunkGeneratorSettings chunkGeneratorSettings = (ChunkGeneratorSettings) this.settings.value();
        noiseConfig.getSurfaceBuilder().buildSurface(noiseConfig, biomeAccess, biomeRegistry, chunkGeneratorSettings.usesLegacyRandom(), heightContext, chunk, chunkNoiseSampler, chunkGeneratorSettings.surfaceRule());
    }

    @Override
    public void carve(ChunkRegion chunkRegion, long seed, NoiseConfig noiseConfig, BiomeAccess biomeAccess, StructureAccessor structureAccessor, Chunk chunk, GenerationStep.Carver carverStep) {
        BiomeAccess biomeAccess2 = biomeAccess.withSource((biomeX, biomeY, biomeZ) -> {
            return this.biomeSource.getBiome(biomeX, biomeY, biomeZ, noiseConfig.getMultiNoiseSampler());
        });
        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
        ChunkPos chunkPos = chunk.getPos();
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler((chunkx) -> {
            return this.createChunkNoiseSampler(chunkx, structureAccessor, Blender.getBlender(chunkRegion), noiseConfig);
        });
        AquiferSampler aquiferSampler = chunkNoiseSampler.getAquiferSampler();
        CarverContext carverContext = new CarverContext(defaultGen, chunkRegion.getRegistryManager(), chunk.getHeightLimitView(), chunkNoiseSampler, noiseConfig, ((ChunkGeneratorSettings) this.settings.value()).surfaceRule());
        CarvingMask carvingMask = ((ProtoChunk) chunk).getOrCreateCarvingMask(carverStep);

        for (int j = -8; j <= 8; ++j) {
            for (int k = -8; k <= 8; ++k) {
                ChunkPos chunkPos2 = new ChunkPos(chunkPos.x + j, chunkPos.z + k);
                Chunk chunk2 = chunkRegion.getChunk(chunkPos2.x, chunkPos2.z);
                GenerationSettings generationSettings = chunk2.getOrCreateGenerationSettings(() -> {
                    return this.getGenerationSettings(this.biomeSource.getBiome(BiomeCoords.fromBlock(chunkPos2.getStartX()), 0, BiomeCoords.fromBlock(chunkPos2.getStartZ()), noiseConfig.getMultiNoiseSampler()));
                });
                Iterable<RegistryEntry<ConfiguredCarver<?>>> iterable = generationSettings.getCarversForStep(carverStep);
                int l = 0;

                for (Iterator var24 = iterable.iterator(); var24.hasNext(); ++l) {
                    RegistryEntry<ConfiguredCarver<?>> registryEntry = (RegistryEntry) var24.next();
                    ConfiguredCarver<?> configuredCarver = (ConfiguredCarver) registryEntry.value();
                    chunkRandom.setCarverSeed(seed + (long) l, chunkPos2.x, chunkPos2.z);
                    if (configuredCarver.shouldCarve(chunkRandom)) {
                        Objects.requireNonNull(biomeAccess2);
                        configuredCarver.carve(carverContext, chunk, biomeAccess2::getBiome, chunkRandom, aquiferSampler, chunkPos2, carvingMask);
                    }
                }
            }
        }

    }

    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        if (chunk.getPos().x == 0 && chunk.getPos().z == 0) {
            System.out.println("hi");
            GenerationShapeConfig generationShapeConfig = ((ChunkGeneratorSettings) this.settings.value()).generationShapeConfig().trimHeight(chunk.getHeightLimitView());
            int i = generationShapeConfig.minimumY();
            int j = MathHelper.floorDiv(i, generationShapeConfig.verticalCellBlockCount());
            int k = MathHelper.floorDiv(generationShapeConfig.height(), generationShapeConfig.verticalCellBlockCount());
            if (k <= 0) {
                return CompletableFuture.completedFuture(chunk);
            } else {
                int l = chunk.getSectionIndex(k * generationShapeConfig.verticalCellBlockCount() - 1 + i);
                int m = chunk.getSectionIndex(i);
                Set<ChunkSection> set = Sets.newHashSet();

                for (int n = l; n >= m; --n) {
                    ChunkSection chunkSection = chunk.getSection(n);
                    chunkSection.lock();
                    set.add(chunkSection);
                }

                return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> {
                    return this.populateNoise(blender, structureAccessor, noiseConfig, chunk, j, k);
                }), Util.getMainWorkerExecutor()).whenCompleteAsync((chunkx, throwable) -> {
                    Iterator var3 = set.iterator();

                    while (var3.hasNext()) {
                        ChunkSection chunkSection = (ChunkSection) var3.next();
                        chunkSection.unlock();
                    }

                }, executor);
            }
        }

        Heightmap heightmap = chunk.getHeightmap(Heightmap.Type.OCEAN_FLOOR_WG);
        Heightmap heightmap2 = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE_WG);

        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (
                int i = 0; i < chunk.getHeight(); ++i) {
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

        for (int o = 0; o < m; ++o) {
            chunkNoiseSampler.sampleEndDensity(o);

            for (int p = 0; p < n; ++p) {
                int q = chunk.countVerticalSections() - 1;
                ChunkSection chunkSection = chunk.getSection(q);

                for (int r = cellHeight - 1; r >= 0; --r) {
                    chunkNoiseSampler.onSampledCellCorners(r, p);

                    for (int s = l - 1; s >= 0; --s) {
                        int t = (minimumCellY + r) * l + s;
                        int u = t & 15;
                        int v = chunk.getSectionIndex(t);
                        if (q != v) {
                            q = v;
                            chunkSection = chunk.getSection(v);
                        }

                        double d = (double) s / (double) l;
                        chunkNoiseSampler.interpolateY(t, d);

                        for (int w = 0; w < k; ++w) {
                            int x = i + o * k + w;
                            int y = x & 15;
                            double e = (double) w / (double) k;
                            chunkNoiseSampler.interpolateX(x, e);

                            for (int z = 0; z < k; ++z) {
                                int aa = j + p * k + z;
                                int ab = aa & 15;
                                double f = (double) z / (double) k;
                                chunkNoiseSampler.interpolateZ(aa, f);
                                BlockState blockState = chunkNoiseSampler.sampleBlockState();
                                if (blockState == null) {
                                    blockState = ((ChunkGeneratorSettings) this.settings.value()).defaultBlock();
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

    /* the distance between the highest and lowest points in the world. in vanilla, this is 384 (64+320) */
    @Override
    public int getWorldHeight() {
        return ((ChunkGeneratorSettings) this.settings.value()).generationShapeConfig().height();
    }

    /* bruh da sea level */
    @Override
    public int getSeaLevel() {
        return ((ChunkGeneratorSettings) this.settings.value()).seaLevel();
    }

    /* bruh duh bedrock ish */
    @Override
    public int getMinimumY() {
        return ((ChunkGeneratorSettings) this.settings.value()).generationShapeConfig().minimumY();
    }

    /* this method spawns entities in the world */
    @Override
    public void populateEntities(ChunkRegion region) {
        if (!((ChunkGeneratorSettings) this.settings.value()).mobGenerationDisabled()) {
            ChunkPos chunkPos = region.getCenterPos();
            RegistryEntry<Biome> registryEntry = region.getBiome(chunkPos.getStartPos().withY(region.getTopY() - 1));
            ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(RandomSeed.getSeed()));
            chunkRandom.setPopulationSeed(region.getSeed(), chunkPos.getStartX(), chunkPos.getStartZ());
            SpawnHelper.populateEntities(region, registryEntry, chunkPos, chunkRandom);
        }
    }
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