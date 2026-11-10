package com.rustcrates.network;

import com.rustcrates.CrateTier;
import com.rustcrates.RustCrates;
import com.rustcrates.block.BlockCrate;
import com.rustcrates.data.CrateData;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Сервер -> клиент: снимок настроек панели управления. */
public class MsgPanelData implements IMessage {
    public boolean open;
    public int tier;
    public boolean enabled, clearBefore, randomCount;
    public int interval, maxItems, secondsLeft;
    public final List<CrateInfo> crates = new ArrayList<>();
    public final List<LootInfo> loot = new ArrayList<>();

    public static class CrateInfo {
        public int dim;
        public BlockPos pos;
        public boolean selected;
        public boolean loaded;
    }

    public static class LootInfo {
        public int slot;
        public ItemStack stack;
        public int chance;
    }

    public MsgPanelData() {}

    public static MsgPanelData build(CrateData data, CrateTier tier, boolean open) {
        CrateData.TierConfig c = data.config(tier);
        MsgPanelData m = new MsgPanelData();
        m.open = open;
        m.tier = tier.ordinal();
        m.enabled = c.enabled;
        m.clearBefore = c.clearBefore;
        m.randomCount = c.randomCount;
        m.interval = c.interval;
        m.maxItems = c.maxItems;
        m.secondsLeft = Math.max(0, (c.interval * 20 - c.ticks) / 20);
        Iterator<CrateData.CrateEntry> it = c.crates.iterator();
        while (it.hasNext()) {
            CrateData.CrateEntry e = it.next();
            World w = DimensionManager.getWorld(e.dim);
            boolean loaded = w != null && w.isBlockLoaded(e.pos);
            if (loaded) {
                Block b = w.getBlockState(e.pos).getBlock();
                if (!(b instanceof BlockCrate) || ((BlockCrate) b).admin || ((BlockCrate) b).tier != tier) {
                    it.remove();
                    data.markDirty();
                    continue;
                }
            }
            CrateInfo ci = new CrateInfo();
            ci.dim = e.dim;
            ci.pos = e.pos;
            ci.selected = e.selected;
            ci.loaded = loaded;
            m.crates.add(ci);
        }
        for (int i = 0; i < c.pool.getSlots(); i++) {
            ItemStack s = c.pool.getStackInSlot(i);
            if (s.isEmpty()) continue;
            LootInfo li = new LootInfo();
            li.slot = i;
            li.stack = s.copy();
            li.chance = c.chances[i];
            m.loot.add(li);
        }
        return m;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeBoolean(open);
        buf.writeByte(tier);
        buf.writeBoolean(enabled);
        buf.writeBoolean(clearBefore);
        buf.writeBoolean(randomCount);
        buf.writeInt(interval);
        buf.writeInt(maxItems);
        buf.writeInt(secondsLeft);
        buf.writeInt(crates.size());
        for (CrateInfo c : crates) {
            buf.writeInt(c.dim);
            buf.writeLong(c.pos.toLong());
            buf.writeBoolean(c.selected);
            buf.writeBoolean(c.loaded);
        }
        buf.writeInt(loot.size());
        for (LootInfo l : loot) {
            buf.writeByte(l.slot);
            ByteBufUtils.writeItemStack(buf, l.stack);
            buf.writeByte(l.chance);
        }
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        open = buf.readBoolean();
        tier = buf.readByte();
        enabled = buf.readBoolean();
        clearBefore = buf.readBoolean();
        randomCount = buf.readBoolean();
        interval = buf.readInt();
        maxItems = buf.readInt();
        secondsLeft = buf.readInt();
        int n = buf.readInt();
        for (int i = 0; i < n; i++) {
            CrateInfo c = new CrateInfo();
            c.dim = buf.readInt();
            c.pos = BlockPos.fromLong(buf.readLong());
            c.selected = buf.readBoolean();
            c.loaded = buf.readBoolean();
            crates.add(c);
        }
        int l = buf.readInt();
        for (int i = 0; i < l; i++) {
            LootInfo li = new LootInfo();
            li.slot = buf.readByte();
            li.stack = ByteBufUtils.readItemStack(buf);
            li.chance = buf.readByte();
            loot.add(li);
        }
    }

    public static class Handler implements IMessageHandler<MsgPanelData, IMessage> {
        @Override
        public IMessage onMessage(MsgPanelData message, MessageContext ctx) {
            RustCrates.proxy.handlePanelData(message);
            return null;
        }
    }
}
