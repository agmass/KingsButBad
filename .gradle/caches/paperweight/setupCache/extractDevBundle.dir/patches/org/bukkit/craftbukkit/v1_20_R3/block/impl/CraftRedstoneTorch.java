/**
 * Automatically generated file, changes will be lost.
 */
package org.bukkit.craftbukkit.v1_20_R3.block.impl;

public final class CraftRedstoneTorch extends org.bukkit.craftbukkit.v1_20_R3.block.data.CraftBlockData implements org.bukkit.block.data.Lightable {

    public CraftRedstoneTorch() {
        super();
    }

    public CraftRedstoneTorch(net.minecraft.world.level.block.state.BlockState state) {
        super(state);
    }

    // org.bukkit.craftbukkit.v1_20_R3.block.data.CraftLightable

    private static final net.minecraft.world.level.block.state.properties.BooleanProperty LIT = getBoolean(net.minecraft.world.level.block.RedstoneTorchBlock.class, "lit");

    @Override
    public boolean isLit() {
        return this.get(CraftRedstoneTorch.LIT);
    }

    @Override
    public void setLit(boolean lit) {
        this.set(CraftRedstoneTorch.LIT, lit);
    }
}
