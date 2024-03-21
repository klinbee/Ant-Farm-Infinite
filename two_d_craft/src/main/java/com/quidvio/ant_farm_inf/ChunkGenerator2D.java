//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package com.quidvio.ant_farm_inf;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Suppliers;
import com.google.common.collect.Sets;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import net.minecraft.SharedConstants;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.*;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.DebugInfoSender;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.structure.StructureSet;
import net.minecraft.structure.StructureStart;
import net.minecraft.structure.StructureTemplateManager;
import net.minecraft.util.Util;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.*;
import net.minecraft.util.math.random.CheckedRandom;
import net.minecraft.util.math.random.ChunkRandom;
import net.minecraft.util.math.random.RandomSeed;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.GenerationSettings;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeCoords;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.BiomeSupplier;
import net.minecraft.world.biome.source.util.MultiNoiseUtil;
import net.minecraft.world.chunk.*;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.HeightContext;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.StructureWeightSampler;
import net.minecraft.world.gen.carver.CarverContext;
import net.minecraft.world.gen.carver.CarvingMask;
import net.minecraft.world.gen.carver.ConfiguredCarver;
import net.minecraft.world.gen.chunk.*;
import net.minecraft.world.gen.chunk.placement.*;
import net.minecraft.world.gen.densityfunction.DensityFunction;
import net.minecraft.world.gen.densityfunction.DensityFunctionTypes;
import net.minecraft.world.gen.densityfunction.DensityFunctions;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.noise.NoiseRouter;
import net.minecraft.world.gen.structure.Structure;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class ChunkGenerator2D extends ChunkGenerator {

    public static final Codec<ChunkGenerator2D> CODEC = RecordCodecBuilder.create((instance) -> instance.group(BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator2D::getBiomeSource), ChunkGeneratorSettings.REGISTRY_CODEC.fieldOf("settings").forGetter(ChunkGenerator2D::getSettings)).apply(instance, instance.stable(ChunkGenerator2D::new)));

    static {
        AIR = Blocks.AIR.getDefaultState();
        BORDER = Ant_farm_inf_main.BORDER_BLOCK.getDefaultState();
    }

    private static final BlockState AIR;
    private static final BlockState BORDER;
    private static final int maxChunkDistFromXAxis = 0; // The max distance, in chunks, the world will generate away from the x-axis.
    private static final int structureChunkDistanceFlexibility = 2; // The max distance, in chunks, the world will generate structures outside of the region.
    public NoiseChunkGenerator defaultGen;
    private final RegistryEntry<ChunkGeneratorSettings> settings;
    private final Supplier<AquiferSampler.FluidLevelSampler> fluidLevelSampler;
    private final BiomeSource biomeSource;

    /**
     * Constructor
     * Added defaultGen setter and field for utility.
     *
     * @param biomeSource
     * @param settings
     */
    public ChunkGenerator2D(BiomeSource biomeSource, RegistryEntry<ChunkGeneratorSettings> settings) {
        super(biomeSource);
        this.settings = settings;
        this.biomeSource = biomeSource;
        this.defaultGen = new NoiseChunkGenerator(biomeSource, settings);
        this.fluidLevelSampler = Suppliers.memoize(() -> createFluidLevelSampler((ChunkGeneratorSettings) settings.value()));

    }

    /**
     * TODO: Unsure if needed
     *
     * @param settings
     * @return
     */
    private static AquiferSampler.FluidLevelSampler createFluidLevelSampler(ChunkGeneratorSettings settings) {
        AquiferSampler.FluidLevel fluidLevel = new AquiferSampler.FluidLevel(-54, Blocks.LAVA.getDefaultState());
        int i = settings.seaLevel();
        AquiferSampler.FluidLevel fluidLevel2 = new AquiferSampler.FluidLevel(i, settings.defaultFluid());
        AquiferSampler.FluidLevel fluidLevel3 = new AquiferSampler.FluidLevel(DimensionType.MIN_HEIGHT * 2, Blocks.AIR.getDefaultState());
        return (x, y, z) -> {
            return y < Math.min(-54, i) ? fluidLevel : fluidLevel2;
        };
    }

    /**
     * TODO: Unsure if needed
     *
     * @param executor
     * @param noiseConfig
     * @param blender
     * @param structureAccessor
     * @param chunk
     * @return
     */
    public CompletableFuture<Chunk> populateBiomes(Executor executor, NoiseConfig noiseConfig, Blender blender, StructureAccessor structureAccessor, Chunk chunk) {
        return CompletableFuture.supplyAsync(Util.debugSupplier("init_biomes", () -> {
            this.populateBiomes(blender, noiseConfig, structureAccessor, chunk);
            return chunk;
        }), Util.getMainWorkerExecutor());
    }

    /**
     * Needed for populateBiomes.
     *
     * @param blender
     * @param noiseConfig
     * @param structureAccessor
     * @param chunk
     */
    private void populateBiomes(Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler((chunkx) -> {
            return this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig);
        });
        BiomeSupplier biomeSupplier = BelowZeroRetrogen.getBiomeSupplier(blender.getBiomeSupplier(this.biomeSource), chunk);
        chunk.populateBiomes(biomeSupplier, chunkNoiseSampler.createMultiNoiseSampler(noiseConfig.getNoiseRouter(), new ArrayList<MultiNoiseUtil.NoiseHypercube>()));
    }

    /**
     * Needed for populateBiomes, populateBiomes.
     *
     * @param chunk
     * @param world
     * @param blender
     * @param noiseConfig
     * @return
     */
    private ChunkNoiseSampler createChunkNoiseSampler(Chunk chunk, StructureAccessor world, Blender blender, NoiseConfig noiseConfig) {
        return ChunkNoiseSampler.create(chunk, noiseConfig, StructureWeightSampler.createStructureWeightSampler(world, chunk.getPos()), (ChunkGeneratorSettings) this.settings.value(), (AquiferSampler.FluidLevelSampler) this.fluidLevelSampler.get(), blender);
    }

    /**
     * Needed for Inheritance.
     *
     * @return
     */
    @Override
    protected Codec<? extends ChunkGenerator> getCodec() {
        return CODEC;
    }

    /**
     * Used by various functions.
     *
     * @return
     */
    public RegistryEntry<ChunkGeneratorSettings> getSettings() {
        return this.settings;
    }

    /**
     * Used by various functions.
     *
     * @return
     */
    public BiomeSource getBiomeSource() {
        return this.biomeSource;
    }

    /**
     * TODO: Unsure if needed
     *
     * @param settings
     * @return
     */
    public boolean matchesSettings(RegistryKey<ChunkGeneratorSettings> settings) {
        return this.settings.matchesKey(settings);
    }

    /**
     * Needed for Inheritance.
     *
     * @param x
     * @param z
     * @param heightmap
     * @param world
     * @param noiseConfig
     * @return
     */
    @Override
    public int getHeight(int x, int z, Heightmap.Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
        return this.sampleHeightmap(world, noiseConfig, x, z, (MutableObject) null, heightmap.getBlockPredicate()).orElse(world.getBottomY());
    }

    /**
     * Needed for Inheritance.
     *
     * @param x
     * @param z
     * @param world
     * @param noiseConfig
     * @return
     */
    @Override
    public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
        MutableObject<VerticalBlockSample> mutableObject = new MutableObject();
        this.sampleHeightmap(world, noiseConfig, x, z, mutableObject, (Predicate) null);
        return (VerticalBlockSample) mutableObject.getValue();
    }

    /**
     * Needed for Inheritance.
     *
     * @param text
     * @param noiseConfig
     * @param pos
     */
    @Override
    public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {
        DecimalFormat decimalFormat = new DecimalFormat("0.000");
        NoiseRouter noiseRouter = noiseConfig.getNoiseRouter();
        DensityFunction.UnblendedNoisePos unblendedNoisePos = new DensityFunction.UnblendedNoisePos(pos.getX(), pos.getY(), pos.getZ());
        double d = noiseRouter.ridges().sample(unblendedNoisePos);
        String var10001 = decimalFormat.format(noiseRouter.temperature().sample(unblendedNoisePos));
        text.add("NoiseRouter T: " + var10001 + " V: " + decimalFormat.format(noiseRouter.vegetation().sample(unblendedNoisePos)) + " C: " + decimalFormat.format(noiseRouter.continents().sample(unblendedNoisePos)) + " E: " + decimalFormat.format(noiseRouter.erosion().sample(unblendedNoisePos)) + " D: " + decimalFormat.format(noiseRouter.depth().sample(unblendedNoisePos)) + " W: " + decimalFormat.format(d) + " PV: " + decimalFormat.format((double) DensityFunctions.getPeaksValleysNoise((float) d)) + " AS: " + decimalFormat.format(noiseRouter.initialDensityWithoutJaggedness().sample(unblendedNoisePos)) + " N: " + decimalFormat.format(noiseRouter.finalDensity().sample(unblendedNoisePos)));
    }

    /**
     * Used by various functions.
     *
     * @param world
     * @param noiseConfig
     * @param x
     * @param z
     * @param columnSample
     * @param stopPredicate
     * @return
     */
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


    /**
     * Uses horizontal limiting (chunkPos < maxDist)
     *
     * @param region
     * @param structures
     * @param noiseConfig
     * @param chunk
     */
    @Override
    public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {
        if (Math.abs(chunk.getPos().x) <= maxChunkDistFromXAxis) {
            if (!SharedConstants.isOutsideGenerationArea(chunk.getPos())) {
                HeightContext heightContext = new HeightContext(this, region);
                this.buildSurface(chunk, heightContext, noiseConfig, structures, region.getBiomeAccess(), region.getRegistryManager().get(RegistryKeys.BIOME), Blender.getBlender(region));
            }
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

    /**
     * Uses defaultGen to generate carvers, as it needs to be a NoiseChunkGenerator.
     *
     * @return
     */
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

    /**
     * Placing terrain
     * Inside of maxChunkDistFromXAxis is default terrain.
     * Outside is just border blocks.
     * <p>
     * Uses horizontal limiting (chunkPos < maxDist)
     *
     * @param executor
     * @param blender
     * @param noiseConfig
     * @param structureAccessor
     * @param chunk
     * @return
     */
    @Override
    public CompletableFuture<Chunk> populateNoise(Executor executor, Blender blender, NoiseConfig noiseConfig, StructureAccessor structureAccessor, Chunk chunk) {
        if (Math.abs(chunk.getPos().x) <= maxChunkDistFromXAxis) {
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

                return CompletableFuture.supplyAsync(Util.debugSupplier("wgen_fill_noise", () -> this.populateNoise(blender, structureAccessor, noiseConfig, chunk, j, k)), Util.getMainWorkerExecutor()).whenCompleteAsync((chunkx, throwable) -> {

                    for (ChunkSection chunkSection : set) {
                        chunkSection.unlock();
                    }

                }, executor);
            }
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

    /**
     * Needed for populateNoise.
     *
     * @param blender
     * @param structureAccessor
     * @param noiseConfig
     * @param chunk
     * @param minimumCellY
     * @param cellHeight
     * @return
     */
    private Chunk populateNoise(Blender blender, StructureAccessor structureAccessor, NoiseConfig noiseConfig, Chunk chunk, int minimumCellY, int cellHeight) {
        ChunkNoiseSampler chunkNoiseSampler = chunk.getOrCreateChunkNoiseSampler((chunkx) -> this.createChunkNoiseSampler(chunkx, structureAccessor, blender, noiseConfig));
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

    /**
     * TODO: Unsure if needed
     *
     * @param chunkNoiseSampler
     * @param x
     * @param y
     * @param z
     * @param state
     * @return
     */
    private BlockState getBlockState(ChunkNoiseSampler chunkNoiseSampler, int x, int y, int z, BlockState state) {
        return state;
    }

    /**
     * Needed for Inheritance.
     *
     * @return
     */
    @Override
    public int getWorldHeight() {
        return ((ChunkGeneratorSettings) this.settings.value()).generationShapeConfig().height();
    }

    /**
     * Needed for Inheritance.
     *
     * @return
     */
    @Override
    public int getSeaLevel() {
        return ((ChunkGeneratorSettings) this.settings.value()).seaLevel();
    }

    /**
     * Needed for Inheritance.
     *
     * @return
     */
    @Override
    public int getMinimumY() {
        return ((ChunkGeneratorSettings) this.settings.value()).generationShapeConfig().minimumY();
    }

    /**
     * Needed for Inheritance.
     *
     * @param region
     */
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

    /**
     * Uses horizontal limiting (chunkPos < maxDist)
     *
     * @param world
     * @param chunk
     * @param structureAccessor
     */
    @Override
    public void generateFeatures(StructureWorldAccess world, Chunk chunk, StructureAccessor structureAccessor) {
        if (Math.abs(chunk.getPos().x) <= maxChunkDistFromXAxis) {
            defaultGen.generateFeatures(world, chunk, structureAccessor);
        }
    }

    /**
     * Noise config made from placement calculator is blank = only river biome structures generate.
     * Additionally I made my own random for generating the structure locations using their system.
     * However, this sucks.
     * <p>
     * Uses horizontal limiting (chunkPos < maxDist)
     *
     * @param registryManager
     * @param placementCalculator
     * @param structureAccessor
     * @param chunk
     * @param structureTemplateManager
     */
    public void setStructureStarts(DynamicRegistryManager registryManager, StructurePlacementCalculator placementCalculator, StructureAccessor structureAccessor, Chunk chunk, StructureTemplateManager structureTemplateManager) {
        if (Math.abs(chunk.getPos().x) <= maxChunkDistFromXAxis + structureChunkDistanceFlexibility) {
            ChunkPos chunkPos = chunk.getPos();
            ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);
            NoiseConfig noiseConfig = placementCalculator.getNoiseConfig();
            placementCalculator.getStructureSets().forEach((structureSet) -> {
                StructurePlacement placement = structureSet.value().placement();
                if (placement instanceof ConcentricRingsStructurePlacement crsp) {
                    placement = new RandomSpreadStructurePlacement(new Vec3i(0, 0, 0), crsp.getFrequencyReductionMethod(), 1.0F, crsp.getSalt(), Optional.empty(), 125, 123, SpreadType.LINEAR);
                } else {
                    Random rand = new Random();
                    int spacing = rand.nextInt(40) + 14;
                    placement = new RandomSpreadStructurePlacement(new Vec3i(0, 0, 0), placement.getFrequencyReductionMethod(), 0.8F, placement.getSalt(), Optional.empty(), spacing, spacing - 2, SpreadType.LINEAR);
                }
                if (Math.abs(chunk.getPos().z) < 4) {
                    placement = new RandomSpreadStructurePlacement(new Vec3i(0, 0, 0), placement.getFrequencyReductionMethod(), 0.0F, 0, Optional.empty(), 1, 0, SpreadType.LINEAR);
                }
                StructurePlacement structurePlacement = placement;
                List<StructureSet.WeightedEntry> list = ((StructureSet) structureSet.value()).structures();
                Iterator var12 = list.iterator();

                while (var12.hasNext()) {
                    StructureSet.WeightedEntry weightedEntry = (StructureSet.WeightedEntry) var12.next();
                    if (chunk.getPos().x == 0 && weightedEntry.structure().value().toString().contains("BuriedTreasure")) {
                        structurePlacement = new RandomSpreadStructurePlacement(new Vec3i(9, 0, 9), placement.getFrequencyReductionMethod(), 0.5F, placement.getSalt(), Optional.empty(), 1, 0, SpreadType.LINEAR);
                    }
                    StructureStart structureStart = structureAccessor.getStructureStart(chunkSectionPos, (Structure) weightedEntry.structure().value(), chunk);
                    if (structureStart != null && structureStart.hasChildren()) {
                        return;
                    }
                }

                if (structurePlacement.shouldGenerate(placementCalculator, chunkPos.x, chunkPos.z)) {
                    if (list.size() == 1) {
                        this.trySetStructureStart((StructureSet.WeightedEntry) list.get(0), structureAccessor, registryManager, noiseConfig, structureTemplateManager, placementCalculator.getStructureSeed(), chunk, chunkPos, chunkSectionPos);
                    } else {
                        ArrayList<StructureSet.WeightedEntry> arrayList = new ArrayList(list.size());
                        arrayList.addAll(list);
                        ChunkRandom chunkRandom = new ChunkRandom(new CheckedRandom(0L));
                        chunkRandom.setCarverSeed(placementCalculator.getStructureSeed(), chunkPos.x, chunkPos.z);
                        int i = 0;

                        StructureSet.WeightedEntry weightedEntry2;
                        for (Iterator var15 = arrayList.iterator(); var15.hasNext(); i += weightedEntry2.weight()) {
                            weightedEntry2 = (StructureSet.WeightedEntry) var15.next();
                        }

                        while (!arrayList.isEmpty()) {
                            int j = chunkRandom.nextInt(i);
                            int k = 0;

                            for (Iterator var17 = arrayList.iterator(); var17.hasNext(); ++k) {
                                StructureSet.WeightedEntry weightedEntry3 = (StructureSet.WeightedEntry) var17.next();
                                j -= weightedEntry3.weight();
                                if (j < 0) {
                                    break;
                                }
                            }

                            StructureSet.WeightedEntry weightedEntry4 = (StructureSet.WeightedEntry) arrayList.get(k);
                            if (this.trySetStructureStart(weightedEntry4, structureAccessor, registryManager, noiseConfig, structureTemplateManager, placementCalculator.getStructureSeed(), chunk, chunkPos, chunkSectionPos)) {
                                return;
                            }

                            arrayList.remove(k);
                            i -= weightedEntry4.weight();
                        }

                    }
                }
            });
        }
    }

    /**
     * This is very important; noiseConfig1 is the fixed noise config with the proper biome parameters.
     *
     * @param weightedEntry
     * @param structureAccessor
     * @param dynamicRegistryManager
     * @param noiseConfig
     * @param structureManager
     * @param seed
     * @param chunk
     * @param pos
     * @param sectionPos
     * @return
     */
    private boolean trySetStructureStart(StructureSet.WeightedEntry weightedEntry, StructureAccessor structureAccessor, DynamicRegistryManager dynamicRegistryManager, NoiseConfig noiseConfig, StructureTemplateManager structureManager, long seed, Chunk chunk, ChunkPos pos, ChunkSectionPos sectionPos) {
        Structure structure = (Structure) weightedEntry.structure().value();
        int i = getStructureReferences(structureAccessor, chunk, sectionPos, structure);
        RegistryEntryList<Biome> registryEntryList = structure.getValidBiomes();
        Objects.requireNonNull(registryEntryList);
        Predicate<RegistryEntry<Biome>> predicate = registryEntryList::contains;
        NoiseConfig noiseConfig1 = NoiseConfig.create((ChunkGeneratorSettings) ((ChunkGeneratorSettings) this.getSettings().value()), (RegistryEntryLookup) dynamicRegistryManager.getWrapperOrThrow(RegistryKeys.NOISE_PARAMETERS), seed);
        StructureStart structureStart = structure.createStructureStart(dynamicRegistryManager, this, this.biomeSource, noiseConfig1, structureManager, seed, pos, i, chunk, predicate);
        if (structureStart.hasChildren()) {
            structureAccessor.setStructureStart(sectionPos, structure, structureStart, chunk);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Needed for trySetStructureStart
     *
     * @param structureAccessor
     * @param chunk
     * @param sectionPos
     * @param structure
     * @return
     */
    private static int getStructureReferences(StructureAccessor structureAccessor, Chunk chunk, ChunkSectionPos sectionPos, Structure structure) {
        StructureStart structureStart = structureAccessor.getStructureStart(sectionPos, structure, chunk);
        return structureStart != null ? structureStart.getReferences() : 0;
    }


    /**
     * TODO: Unsure if needed
     * Finds all structures that the given chunk intersects, and adds references to their starting chunks to it.
     * A radius of 8 chunks around the given chunk will be searched for structure starts.
     */
    public void addStructureReferences(StructureWorldAccess world, StructureAccessor structureAccessor, Chunk chunk) {
        ChunkPos chunkPos = chunk.getPos();
        int j = chunkPos.x;
        int k = chunkPos.z;
        int l = chunkPos.getStartX();
        int m = chunkPos.getStartZ();
        ChunkSectionPos chunkSectionPos = ChunkSectionPos.from(chunk);

        for (int n = j - 8; n <= j + 8; ++n) {
            for (int o = k - 8; o <= k + 8; ++o) {
                long p = ChunkPos.toLong(n, o);
                Iterator var15 = world.getChunk(n, o).getStructureStarts().values().iterator();

                while (var15.hasNext()) {
                    StructureStart structureStart = (StructureStart) var15.next();

                    try {
                        if (structureStart.hasChildren() && structureStart.getBoundingBox().intersectsXZ(l, m, l + 15, m + 15)) {
                            structureAccessor.addStructureReference(chunkSectionPos, structureStart.getStructure(), p, chunk);
                            DebugInfoSender.sendStructureStart(world, structureStart);
                        }
                    } catch (Exception var21) {
                        CrashReport crashReport = CrashReport.create(var21, "Generating structure reference");
                        CrashReportSection crashReportSection = crashReport.addElement("Structure");
                        Optional<? extends Registry<Structure>> optional = world.getRegistryManager().getOptional(RegistryKeys.STRUCTURE);
                        crashReportSection.add("Id", () -> {
                            return (String) optional.map((structureTypeRegistry) -> {
                                return structureTypeRegistry.getId(structureStart.getStructure()).toString();
                            }).orElse("UNKNOWN");
                        });
                        crashReportSection.add("Name", () -> {
                            return Registries.STRUCTURE_TYPE.getId(structureStart.getStructure().getType()).toString();
                        });
                        crashReportSection.add("Class", () -> {
                            return structureStart.getStructure().getClass().getCanonicalName();
                        });
                        throw new CrashException(crashReport);
                    }
                }
            }
        }
    }

    /**
     * TODO: Fix later this sucks
     * Added to change how searching works to accomodate for strongholds mainly... it sucks. Need to make my own rng system and structure placement.
     * No more concentric rings structure searching.
     *
     * @param world
     * @param structures
     * @param center
     * @param radius
     * @param skipReferencedStructures
     * @return
     */
    @Nullable
    public Pair<BlockPos, RegistryEntry<Structure>> locateStructure(ServerWorld world, RegistryEntryList<Structure> structures, BlockPos center, int radius, boolean skipReferencedStructures) {
        StructurePlacementCalculator structurePlacementCalculator = world.getChunkManager().getStructurePlacementCalculator();
        Map<StructurePlacement, Set<RegistryEntry<Structure>>> map = new Object2ObjectArrayMap();
        Iterator var8 = structures.iterator();

        while (var8.hasNext()) {
            RegistryEntry<Structure> registryEntry = (RegistryEntry) var8.next();
            Iterator var10 = structurePlacementCalculator.getPlacements(registryEntry).iterator();

            while (var10.hasNext()) {
                StructurePlacement structurePlacement = (StructurePlacement) var10.next();
                ((Set) map.computeIfAbsent(structurePlacement, (placement) -> {
                    return new ObjectArraySet();
                })).add(registryEntry);
            }
        }

        if (map.isEmpty()) {
            return null;
        } else {
            Pair<BlockPos, RegistryEntry<Structure>> pair = null;
            double d = 1.7976931348623157E308D;
            StructureAccessor structureAccessor = world.getStructureAccessor();
            List<Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>>> list = new ArrayList(map.size());
            Iterator var13 = map.entrySet().iterator();

            while (var13.hasNext()) {
                Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry = (Map.Entry) var13.next();
                StructurePlacement structurePlacement2 = (StructurePlacement) entry.getKey();
                list.add(entry);
            }

            if (!list.isEmpty()) {
                int i = ChunkSectionPos.getSectionCoord(center.getX());
                int j = ChunkSectionPos.getSectionCoord(center.getZ());

                boolean bl = false;
                Iterator var30 = list.iterator();

                while (var30.hasNext()) {
                    Map.Entry<StructurePlacement, Set<RegistryEntry<Structure>>> entry2 = (Map.Entry) var30.next();
                    StructurePlacement structurePlacement = entry2.getKey();
                    Pair<BlockPos, RegistryEntry<Structure>> pair3 = locateRandomSpreadStructure((Set) entry2.getValue(), world, structureAccessor, i, j, 128, skipReferencedStructures, structurePlacementCalculator.getStructureSeed(), structurePlacement);
                    if (pair3 != null) {
                        bl = true;
                        double f = center.getSquaredDistance((Vec3i) pair3.getFirst());
                        if (f < d) {
                            d = f;
                            pair = pair3;
                        }
                    }
                }

                if (bl) {
                    return pair;
                }

            }

            return pair;
        }
    }

    /**
     * The search alg that is okay, it does it's job.
     * Basic BFS
     * <p>
     * Uses horizontal limiting (chunkPos < maxDist)
     *
     * @param structures
     * @param world
     * @param structureAccessor
     * @param centerChunkX
     * @param centerChunkZ
     * @param zMax
     * @param skipReferencedStructures
     * @param seed
     * @param placement
     * @return
     */
    @Nullable
    private static Pair<BlockPos, RegistryEntry<Structure>> locateRandomSpreadStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, int centerChunkX, int centerChunkZ, int zMax, boolean skipReferencedStructures, long seed, StructurePlacement placement) {
        int k = 0;
        while (k <= zMax) {
            int j = 0;
            while (j <= (maxChunkDistFromXAxis + structureChunkDistanceFlexibility)) {
                Pair<BlockPos, RegistryEntry<Structure>> pair = locateStructure(structures, world, structureAccessor, skipReferencedStructures, placement, new ChunkPos(centerChunkX + j, centerChunkZ + k));
                if (pair != null) {
                    return pair;
                }
                j = j >= 0 ? -j - 1 : -j;
            }
            k = k >= 0 ? -k - 1 : -k;
        }
        return null;
    }

    /**
     * Needed for locateRandomSpreadStructure.
     *
     * @param structures
     * @param world
     * @param structureAccessor
     * @param skipReferencedStructures
     * @param placement
     * @param pos
     * @return
     */
    @Nullable
    private static Pair<BlockPos, RegistryEntry<Structure>> locateStructure(Set<RegistryEntry<Structure>> structures, WorldView world, StructureAccessor structureAccessor, boolean skipReferencedStructures, StructurePlacement placement, ChunkPos pos) {
        Iterator var6 = structures.iterator();
        RegistryEntry registryEntry;
        StructureStart structureStart;
        do {
            do {
                do {
                    StructurePresence structurePresence;
                    do {
                        if (!var6.hasNext()) {
                            return null;
                        }

                        registryEntry = (RegistryEntry) var6.next();
                        structurePresence = structureAccessor.getStructurePresence(pos, (Structure) registryEntry.value(), skipReferencedStructures);
                    } while (structurePresence == StructurePresence.START_NOT_PRESENT);

                    if (!skipReferencedStructures && structurePresence == StructurePresence.START_PRESENT) {
                        return Pair.of(placement.getLocatePos(pos), registryEntry);
                    }

                    Chunk chunk = world.getChunk(pos.x, pos.z, ChunkStatus.STRUCTURE_STARTS);
                    structureStart = structureAccessor.getStructureStart(ChunkSectionPos.from(chunk), (Structure) registryEntry.value(), chunk);
                } while (structureStart == null);
            } while (!structureStart.hasChildren());
        } while (skipReferencedStructures && !checkNotReferenced(structureAccessor, structureStart));

        return Pair.of(placement.getLocatePos(structureStart.getPos()), registryEntry);
    }

    /**
     * Needed for locateRandomSpreadStructure, locateStructure.
     *
     * @param structureAccessor
     * @param start
     * @return
     */
    private static boolean checkNotReferenced(StructureAccessor structureAccessor, StructureStart start) {
        if (start.isNeverReferenced()) {
            structureAccessor.incrementReferences(start);
            return true;
        } else {
            return false;
        }
    }
}