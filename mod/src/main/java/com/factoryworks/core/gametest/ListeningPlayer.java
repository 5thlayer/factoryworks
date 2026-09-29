package com.factoryworks.core.gametest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.neoforged.neoforge.common.util.FakePlayer;

/** A player of its own, keeping the translation key of every message it is sent. */
final class ListeningPlayer extends FakePlayer {
    final List<String> heard = new ArrayList<>();

    ListeningPlayer(GameTestHelper helper) {
        super(helper.getLevel(), new GameProfile(UUID.randomUUID(), "pf_belt_listener"));
    }

    @Override
    public void sendSystemMessage(Component message, boolean actionBar) {
        if (message.getContents() instanceof TranslatableContents translatable) heard.add(translatable.getKey());
    }
}
