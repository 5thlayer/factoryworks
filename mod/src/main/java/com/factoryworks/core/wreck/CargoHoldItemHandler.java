package com.factoryworks.core.wreck;

import com.factoryworks.core.transfer.GuardedResourceHandler;

import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.item.VanillaContainerWrapper;

/** The cargo hold's item face (ADR-0107): every slot takes and gives, on every side. */
public class CargoHoldItemHandler extends GuardedResourceHandler<ItemResource> {

    public CargoHoldItemHandler(CargoHoldBlockEntity hold) {
        super(VanillaContainerWrapper.of(hold));
    }
}
