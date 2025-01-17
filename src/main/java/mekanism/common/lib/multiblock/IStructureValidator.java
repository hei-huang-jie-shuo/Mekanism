package mekanism.common.lib.multiblock;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import java.util.Set;
import mekanism.common.lib.math.voxel.IShape;
import mekanism.common.lib.multiblock.FormationProtocol.FormationResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;

public interface IStructureValidator<T extends MultiblockData> {

    void init(Level world, MultiblockManager<T> manager, Structure structure);

    boolean precheck();

    FormationResult validate(FormationProtocol<T> ctx, Long2ObjectMap<ChunkAccess> chunkMap);

    FormationResult postcheck(T structure, Set<BlockPos> innerNodes, Long2ObjectMap<ChunkAccess> chunkMap);

    IShape getShape();
}
