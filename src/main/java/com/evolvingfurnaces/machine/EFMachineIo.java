package com.evolvingfurnaces.machine;

import com.evolvingfurnaces.tier.EFIoRole;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

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

    public void save(ValueOutput output) {
        ValueOutput.ValueOutputList list = output.childrenList("Io");
        for (int i = 0; i < 6; i++) {
            ValueOutput entry = list.addChild();
            entry.putString("face", Direction.from3DDataValue(i).getSerializedName());
            entry.putString("role", roles[i].getSerializedName());
        }
    }

    public void load(ValueInput input) {
        resetDefaults();
        for (ValueInput entry : input.childrenListOrEmpty("Io")) {
            Direction direction = Direction.byName(entry.getStringOr("face", ""));
            if (direction == null) {
                continue;
            }
            try {
                set(direction, EFIoRole.valueOf(entry.getStringOr("role", "").toUpperCase(Locale.ROOT)));
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
