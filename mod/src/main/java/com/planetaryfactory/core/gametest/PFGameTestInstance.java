package com.planetaryfactory.core.gametest;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

/**
 * A GameTest whose body is a method in this jar rather than a datapack function.
 *
 * <h2>Why this class exists at all</h2>
 *
 * <p>26.1 has no {@code @GameTestHolder}. A test is an entry in the {@code test_instance} datapack
 * registry, and the two shapes vanilla ships are {@code block_based} -- a structure with a test
 * block in it -- and {@code function}, which runs a {@link Consumer} looked up in the
 * {@code test_function} registry. The function registry is populated during {@code Bootstrap}, and
 * mods are loaded after it, so a mod cannot put anything in it. NeoForge's answer is
 * {@link net.neoforged.neoforge.event.RegisterGameTestsEvent}, which hands a mod the instance
 * registry directly; what it does not hand over is a shape to register, so this is that shape.
 *
 * <h2>Why it carries a codec it will almost never use</h2>
 *
 * <p>Every {@link GameTestInstance} must name a codec, because the registry it lives in is a
 * datapack registry and can be encoded -- to a client that asks for the test list, or by
 * {@code /test export}. Nothing on a headless run encodes one. Declaring the codec anyway is
 * cheaper than discovering on the day someone opens a dev client that the registry cannot be
 * synced; it encodes the id and looks the body back up, because a {@link Consumer} is not data.
 */
public class PFGameTestInstance extends GameTestInstance {

    /**
     * Test bodies by id, for the codec to resolve against.
     *
     * <p>Static and global, which is the same shape vanilla's {@code test_function} registry has
     * and for the same reason: the codec is handed an id and nothing else.
     */
    private static final Map<Identifier, Consumer<GameTestHelper>> BODIES = new HashMap<>();

    public static final MapCodec<PFGameTestInstance> CODEC = RecordCodecBuilder.mapCodec(
            instance -> instance.group(
                    Identifier.CODEC.fieldOf("body").forGetter(test -> test.id),
                    TestData.CODEC.forGetter(PFGameTestInstance::info))
                    .apply(instance, PFGameTestInstance::new));

    private final Identifier id;
    private final Consumer<GameTestHelper> body;

    public PFGameTestInstance(Identifier id, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.id = id;
        Consumer<GameTestHelper> found = BODIES.get(id);
        if (found == null) {
            // A test that silently does nothing passes, which is the one failure a test harness
            // must not have.
            throw new IllegalStateException("no game test body registered for " + id);
        }
        this.body = found;
    }

    /** Names a body, before any instance that refers to it is built. */
    public static void define(Identifier id, Consumer<GameTestHelper> body) {
        Consumer<GameTestHelper> previous = BODIES.put(id, body);
        if (previous != null) {
            throw new IllegalStateException("two game test bodies registered for " + id);
        }
    }

    @Override
    public void run(GameTestHelper helper) {
        body.accept(helper);
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("planetary factory");
    }
}
