package mekanism.generators.common.tile.turbine;

import java.util.UUID;
import mekanism.common.tile.prefab.TileEntityInternalMultiblock;
import mekanism.common.util.WorldUtils;
import mekanism.generators.common.content.turbine.TurbineMultiblockData;
import mekanism.generators.common.registries.GeneratorsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class TileEntityRotationalComplex extends TileEntityInternalMultiblock {

    public TileEntityRotationalComplex(BlockPos pos, BlockState state) {
        super(GeneratorsBlocks.ROTATIONAL_COMPLEX, pos, state);
    }

    @Override
    public void setMultiblock(UUID id) {
        if (id == null && multiblockUUID != null) {
            TurbineMultiblockData.clientRotationMap.removeFloat(multiblockUUID);
        }
        super.setMultiblock(id);
        if (!isRemote()) {
            TileEntityTurbineRotor tile = WorldUtils.getTileEntity(TileEntityTurbineRotor.class, getLevel(), getBlockPos().below());
            if (tile != null) {
                tile.updateRotors();
            }
        }
    }
}