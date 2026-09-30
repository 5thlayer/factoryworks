package com.factoryworks.core.gametest;

import com.factoryworks.core.showcase.ShowcaseScenes;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;

/** Each showcase scene makes its product once built (#538); run under `factoryworks_showcase:*`. */
final class ShowcaseSceneTests {

    private ShowcaseSceneTests() {
    }

    static void register(PFGameTests.Registrar tests) {
        for (String scene : ShowcaseScenes.SCENES.keySet()) {
            tests.showcase(scene, 4800, helper -> makesItsProduct(helper, scene));
        }
    }

    private static void makesItsProduct(GameTestHelper helper, String scene) {
        BlockPos origin = helper.absolutePos(BlockPos.ZERO);
        ShowcaseScenes.Product product = ShowcaseScenes.build(scene, helper.getLevel(), origin);
        helper.succeedWhen(() -> {
            if (!product.arrived(helper.getLevel(), origin)) {
                throw helper.assertionException(BlockPos.ZERO, "no " + product.item() + " has arrived");
            }
        });
    }
}
