package com.rustcrates;

import com.rustcrates.client.GuiCrate;
import com.rustcrates.data.CrateData;
import com.rustcrates.inventory.ContainerCrate;
import com.rustcrates.tile.TileCrate;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.items.ItemStackHandler;

public class GuiHandler implements IGuiHandler {
    public static final int GUI_CRATE = 0;
    /** GUI_POOL + номер типа (0..2) — редактирование предметов лута. */
    public static final int GUI_POOL = 10;

    @Override
    public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        if (id == GUI_CRATE) {
            final BlockPos pos = new BlockPos(x, y, z);
            final TileEntity te = world.getTileEntity(pos);
            if (te instanceof TileCrate) {
                return new ContainerCrate(player.inventory, ((TileCrate) te).inventory,
                        p -> !te.isInvalid() && p.getDistanceSq(x + 0.5, y + 0.5, z + 0.5) <= 64.0,
                        te::markDirty);
            }
        } else if (id >= GUI_POOL && id < GUI_POOL + CrateTier.values().length) {
            final CrateData data = CrateData.get();
            CrateTier tier = CrateTier.byId(id - GUI_POOL);
            return new ContainerCrate(player.inventory, data.config(tier).pool, AdminUtil::isAdmin, data::markDirty);
        }
        return null;
    }

    @Override
    public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
        ContainerCrate c = new ContainerCrate(player.inventory, new ItemStackHandler(ContainerCrate.SIZE), p -> true, () -> {});
        if (id == GUI_CRATE) {
            String title = world.getBlockState(new BlockPos(x, y, z)).getBlock().getLocalizedName();
            return new GuiCrate(c, player.inventory, title);
        } else if (id >= GUI_POOL && id < GUI_POOL + CrateTier.values().length) {
            CrateTier tier = CrateTier.byId(id - GUI_POOL);
            return new GuiCrate(c, player.inventory, "Пул лута: " + tier.title);
        }
        return null;
    }
}
