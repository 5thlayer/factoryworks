package com.factoryworks.core.compat;

import com.factoryworks.core.machine.footprint.FootprintPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.Accessor;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

/**
 * A footprint machine's part is read as its anchor, header and icon included, so every block of the
 * machine shows the anchor's line. The part has no block entity and no item, so without this Jade
 * names "(part)" with a blank icon (#333).
 */
@WailaPlugin
public class FootprintJadePlugin implements IWailaPlugin {

    private static Accessor<?> anchorFor(IWailaClientRegistration registration, Accessor<?> accessor) {
        if (!(accessor instanceof BlockAccessor block)
                || !(block.getBlock() instanceof FootprintPartBlock part)) {
            return accessor;
        }
        BlockPos anchor = part.machine().anchorOf(block.getPosition(), block.getBlockState());
        BlockState anchorState = block.getLevel().getBlockState(anchor);
        if (!part.machine().isAnchor(anchorState)) {
            return accessor;
        }
        return registration.blockAccessor()
                .from(block)
                .hit(block.getHitResult().withPosition(anchor))
                .blockState(anchorState)
                .blockEntity(block.getLevel().getBlockEntity(anchor))
                .build();
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addRayTraceCallback((hit, accessor, original) -> anchorFor(registration, accessor));
    }
}
