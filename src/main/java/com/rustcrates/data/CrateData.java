package com.rustcrates.data;

import com.rustcrates.CrateTier;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.storage.MapStorage;
import net.minecraft.world.storage.WorldSavedData;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;

/** Все настройки лута и список поставленных ящиков. Сохраняется в папке мира (data/rustcrates.dat). */
public class CrateData extends WorldSavedData {
    public static final String NAME = "rustcrates";
    private final EnumMap<CrateTier, TierConfig> configs = new EnumMap<>(CrateTier.class);

    public CrateData() {
        this(NAME);
    }

    public CrateData(String name) {
        super(name);
        for (CrateTier t : CrateTier.values()) configs.put(t, new TierConfig(this));
    }

    public static CrateData get() {
        World overworld = DimensionManager.getWorld(0);
        MapStorage storage = overworld.getMapStorage();
        CrateData data = (CrateData) storage.getOrLoadData(CrateData.class, NAME);
        if (data == null) {
            data = new CrateData();
            storage.setData(NAME, data);
        }
        return data;
    }

    public TierConfig config(CrateTier t) {
        return configs.get(t);
    }

    public void addCrate(CrateTier t, int dim, BlockPos pos) {
        TierConfig c = config(t);
        if (c.find(dim, pos) == null) {
            c.crates.add(new CrateEntry(dim, pos.toImmutable(), true));
            markDirty();
        }
    }

    public void removeCrate(CrateTier t, int dim, BlockPos pos) {
        if (config(t).crates.removeIf(e -> e.dim == dim && e.pos.equals(pos))) markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        for (CrateTier t : CrateTier.values()) {
            if (nbt.hasKey(t.id)) config(t).read(nbt.getCompoundTag(t.id));
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        for (CrateTier t : CrateTier.values()) nbt.setTag(t.id, config(t).write());
        return nbt;
    }

    public static class CrateEntry {
        public final int dim;
        public final BlockPos pos;
        public boolean selected;

        public CrateEntry(int dim, BlockPos pos, boolean selected) {
            this.dim = dim;
            this.pos = pos;
            this.selected = selected;
        }
    }

    public static class TierConfig {
        public static final int POOL = 27;
        public final ItemStackHandler pool;
        public final int[] chances = new int[POOL];
        public boolean enabled = true;
        public int interval = 300;
        public boolean clearBefore = true;
        public boolean randomCount = true;
        public int maxItems = 0;
        public final List<CrateEntry> crates = new ArrayList<>();
        public int ticks = 0;

        TierConfig(final CrateData owner) {
            pool = new ItemStackHandler(POOL) {
                @Override
                protected void onContentsChanged(int slot) {
                    owner.markDirty();
                }
            };
            Arrays.fill(chances, 50);
        }

        public CrateEntry find(int dim, BlockPos pos) {
            for (CrateEntry e : crates) if (e.dim == dim && e.pos.equals(pos)) return e;
            return null;
        }

        void read(NBTTagCompound n) {
            if (n.hasKey("Pool")) pool.deserializeNBT(n.getCompoundTag("Pool"));
            if (pool.getSlots() != POOL) pool.setSize(POOL);
            int[] ch = n.getIntArray("Chances");
            for (int i = 0; i < Math.min(ch.length, POOL); i++) chances[i] = ch[i];
            if (n.hasKey("Enabled")) enabled = n.getBoolean("Enabled");
            if (n.hasKey("Interval")) interval = n.getInteger("Interval");
            if (n.hasKey("Clear")) clearBefore = n.getBoolean("Clear");
            if (n.hasKey("Random")) randomCount = n.getBoolean("Random");
            maxItems = n.getInteger("Max");
            ticks = n.getInteger("Ticks");
            crates.clear();
            NBTTagList list = n.getTagList("Crates", Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound c = list.getCompoundTagAt(i);
                crates.add(new CrateEntry(c.getInteger("Dim"), BlockPos.fromLong(c.getLong("Pos")), c.getBoolean("Sel")));
            }
        }

        NBTTagCompound write() {
            NBTTagCompound n = new NBTTagCompound();
            n.setTag("Pool", pool.serializeNBT());
            n.setIntArray("Chances", chances);
            n.setBoolean("Enabled", enabled);
            n.setInteger("Interval", interval);
            n.setBoolean("Clear", clearBefore);
            n.setBoolean("Random", randomCount);
            n.setInteger("Max", maxItems);
            n.setInteger("Ticks", ticks);
            NBTTagList list = new NBTTagList();
            for (CrateEntry e : crates) {
                NBTTagCompound c = new NBTTagCompound();
                c.setInteger("Dim", e.dim);
                c.setLong("Pos", e.pos.toLong());
                c.setBoolean("Sel", e.selected);
                list.appendTag(c);
            }
            n.setTag("Crates", list);
            return n;
        }
    }
}
