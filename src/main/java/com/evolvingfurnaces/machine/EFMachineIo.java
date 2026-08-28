package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFIoRole;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class EFMachineIo {

    private final EFIoRole[] roles = new EFIoRole[6];

    public EFMachineIo() {
        resetDefaults();
    }

    public void resetDefaults() {
        set(Direction.UP, EFIoRole.INPUT);
        set(Direction.DOWN, EFIoRole.OUTPUT);
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            set(direction, EFIoRole.FUEL);
        }
    }

    public EFIoRole get(Direction direction) {
        return roles[direction.get3DDataValue()];
    }

    public void set(Direction direction, EFIoRole role) {
        roles[direction.get3DDataValue()] = role;
    }

    public void applyModuleDefaults(EFEnergyModule module) {
        switch (module) {
            case GENERATOR -> {
                set(Direction.UP, EFIoRole.FUEL);
                set(Direction.DOWN, EFIoRole.ENERGY);
            }
            case ELECTRIC -> {
                set(Direction.UP, EFIoRole.INPUT);
                set(Direction.DOWN, EFIoRole.OUTPUT);
            }
            case NONE -> {
                resetDefaults();
                return;
            }
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            set(direction, EFIoRole.ENERGY);
        }
    }

    public EFIoRole cycle(Direction direction) {
        return cycle(direction, EFEnergyModule.NONE);
    }

    public EFIoRole cycle(Direction direction, EFEnergyModule module) {
        EFIoRole next = nextRole(get(direction), module);
        set(direction, next);
        return next;
    }

    private static EFIoRole nextRole(EFIoRole current, EFEnergyModule module) {
        EFIoRole[] values = EFIoRole.values();
        EFIoRole candidate = current;
        for (int i = 0; i < values.length; i++) {
            candidate = values[(candidate.ordinal() + 1) % values.length];
            if (allowed(candidate, module)) {
                return candidate;
            }
        }
        return EFIoRole.DISABLED;
    }

    private static boolean allowed(EFIoRole role, EFEnergyModule module) {
        return switch (role) {
            case INPUT, OUTPUT -> module != EFEnergyModule.GENERATOR;
            case FUEL -> module != EFEnergyModule.ELECTRIC;
            case ENERGY -> module.installed();
            case DISABLED -> true;
        };
    }

    public int pack() {
        int packed = 0;
        for (int i = 0; i < 6; i++) {
            packed |= roles[i].ordinal() << (i * 3);
        }
        return packed;
    }

    public void unpack(int packed) {
        EFIoRole[] values = EFIoRole.values();
        for (int i = 0; i < 6; i++) {
            int ordinal = (packed >> (i * 3)) & 7;
            roles[i] = ordinal < values.length ? values[ordinal] : EFIoRole.DISABLED;
        }
    }

    public void save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (int i = 0; i < 6; i++) {
            CompoundTag entry = new CompoundTag();
            entry.putString("face", Direction.from3DDataValue(i).getSerializedName());
            entry.putString("role", roles[i].getSerializedName());
            list.add(entry);
        }
        tag.put("Io", list);
    }

    public void load(CompoundTag tag) {
        resetDefaults();
        if (!tag.contains("Io", Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList("Io", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            Direction direction = Direction.byName(entry.getString("face"));
            if (direction == null) {
                continue;
            }
            try {
                set(direction, EFIoRole.valueOf(entry.getString("role").toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                // keep default
            }
        }
    }

    public EFMachineIo copy() {
        EFMachineIo copy = new EFMachineIo();
        System.arraycopy(roles, 0, copy.roles, 0, 6);
        return copy;
    }
}
