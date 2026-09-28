package com.planetaryfactory.core.gametest;

import java.util.UUID;

import com.portingdeadmods.researchd.ResearchdRegistries;
import com.portingdeadmods.researchd.api.ResearchdApi;
import com.portingdeadmods.researchd.api.research.Research;
import com.portingdeadmods.researchd.api.team.ResearchTeam;
import com.portingdeadmods.researchd.api.team.ResearchTeamManager;
import com.portingdeadmods.researchd.api.team.ResearchTeamRole;
import com.portingdeadmods.researchd.data.ResearchdAttachments;
import com.portingdeadmods.researchd.utils.researches.ResearchTeamHelperServer;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Researchd teams for the pack's GameTests; referenced only by tests registered with Researchd loaded. */
final class ResearchTeams {

    private ResearchTeams() {
    }

    /** A new team with nothing researched, so every research's recipes are Blocked for it. */
    static ResearchTeam create(GameTestHelper helper) {
        ResearchTeamManager teams = ResearchdApi.getTeamManager(helper.getLevel());
        UUID owner = UUID.randomUUID();
        ResearchTeam team = teams.createEmptyTeam("GameTest " + owner);
        team.addMember(owner, ResearchTeamRole.OWNER);
        team.init(helper.getLevel());
        teams.addTeam(team);
        ResearchTeamHelperServer.initializeTeamEffects(team, helper.getLevel());
        return team;
    }

    /** What Researchd's complete command does, minus the packets. */
    static void complete(GameTestHelper helper, ResearchTeam team, String research) {
        ResourceKey<Research> key = ResourceKey.create(ResearchdRegistries.RESEARCH_KEY, Identifier.parse(research));
        team.setResearchCompleted(key, helper.getLevel().getGameTime());
        ResearchdApi.getResearchManager().lookupResearch(key, helper.getLevel()).researchEffect()
                .onUnlock(helper.getLevel(), team, key);
    }

    /** As Researchd's placement handler stamps a block a team member places. */
    static void placedBy(BlockEntity machine, ResearchTeam team) {
        machine.setData(ResearchdAttachments.PLACED_BY_UUID, team.getId());
    }
}
