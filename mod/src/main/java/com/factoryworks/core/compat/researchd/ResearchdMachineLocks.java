package com.factoryworks.core.compat.researchd;

import java.util.UUID;

import com.portingdeadmods.researchd.api.ResearchdApi;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * The {@code researchd} lock source: a machine asks for the team that placed it. Loaded only when
 * Researchd is. A machine no team placed is asked for no team, and Researchd blocks nothing for it.
 */
public final class ResearchdMachineLocks {

    private ResearchdMachineLocks() {
    }

    public static boolean isLocked(BlockEntity machine, Identifier recipe) {
        Level level = machine.getLevel();
        UUID team = level == null ? null : ResearchdApi.getOrMigratePlacedByTeam(machine, level);
        return team != null && ResearchdApi.isRecipeBlocked(level, team, ResourceKey.create(Registries.RECIPE, recipe));
    }
}
