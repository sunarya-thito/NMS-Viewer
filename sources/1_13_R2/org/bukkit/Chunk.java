package org.bukkit;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.jetbrains.annotations.NotNull;

/**
 * Represents a chunk of blocks
 */
public interface Chunk {

    /**
     * Gets the X-coordinate of this chunk
     *
     * @return X-coordinate
     */
    int getX();

    /**
     * Gets the Z-coordinate of this chunk
     *
     * @return Z-coordinate
     */
    int getZ();

    /**
     * Gets the world containing this chunk
     *
     * @return Parent World
     */
    @NotNull
    World getWorld();

    /**
     * Gets a block from this chunk
     *
     * @param x 0-15
     * @param y 0-255
     * @param z 0-15
     * @return the Block
     */
    @NotNull
    Block getBlock(int x, int y, int z);

    /**
     * Capture thread-safe read-only snapshot of chunk data
     *
     * @return ChunkSnapshot
     */
    @NotNull
    ChunkSnapshot getChunkSnapshot();

    /**
     * Capture thread-safe read-only snapshot of chunk data
     *
     * @param includeMaxblocky - if true, snapshot includes per-coordinate
     *     maximum Y values
     * @param includeBiome - if true, snapshot includes per-coordinate biome
     *     type
     * @param includeBiomeTempRain - if true, snapshot includes per-coordinate
     *     raw biome temperature and rainfall
     * @return ChunkSnapshot
     */
    @NotNull
    ChunkSnapshot getChunkSnapshot(boolean includeMaxblocky, boolean includeBiome, boolean includeBiomeTempRain);

    /**
     * Get a list of all entities in the chunk.
     *
     * @return The entities.
     */
    @NotNull
    Entity[] getEntities();

    /**
     * Get a list of all tile entities in the chunk.
     *
     * @return The tile entities.
     */
    @NotNull
    BlockState[] getTileEntities();

    /**
     * Checks if the chunk is loaded.
     *
     * @return True if it is loaded.
     */
    boolean isLoaded();

    /**
     * Loads the chunk.
     *
     * @param generate Whether or not to generate a chunk if it doesn't
     *     already exist
     * @return true if the chunk has loaded successfully, otherwise false
     */
    boolean load(boolean generate);

    /**
     * Loads the chunk.
     *
     * @return true if the chunk has loaded successfully, otherwise false
     */
    boolean load();

    /**
     * Unloads and optionally saves the Chunk
     *
     * @param save Controls whether the chunk is saved
     * @param safe Controls whether to unload the chunk when players are
     *     nearby
     * @return true if the chunk has unloaded successfully, otherwise false
     * @deprecated it is never safe to remove a chunk in use
     */
    @Deprecated
    boolean unload(boolean save, boolean safe);

    /**
     * Unloads and optionally saves the Chunk
     *
     * @param save Controls whether the chunk is saved
     * @return true if the chunk has unloaded successfully, otherwise false
     */
    boolean unload(boolean save);

    /**
     * Unloads and optionally saves the Chunk
     *
     * @return true if the chunk has unloaded successfully, otherwise false
     */
    boolean unload();

    /**
     * Checks if this chunk can spawn slimes without being a swamp biome.
     *
     * @return true if slimes are able to spawn in this chunk
     */
    boolean isSlimeChunk();

    /**
     * Gets whether the chunk at the specified chunk coordinates is force
     * loaded.
     * <p>
     * A force loaded chunk will not be unloaded due to lack of player activity.
     *
     * @return force load status
     * @see World#isChunkForceLoaded(int, int)
     */
    boolean isForceLoaded();

    /**
     * Sets whether the chunk at the specified chunk coordinates is force
     * loaded.
     * <p>
     * A force loaded chunk will not be unloaded due to lack of player activity.
     *
     * @param forced
     * @see World#setChunkForceLoaded(int, int, boolean)
     */
    void setForceLoaded(boolean forced);
}
