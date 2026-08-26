package com.evolvingfurnaces.machine;

import net.neoforged.neoforge.energy.IEnergyStorage;

public final class EFMachineEnergy implements IEnergyStorage {

    private final EvolvingMachineBlockEntity machine;
    private int energy;

    public EFMachineEnergy(EvolvingMachineBlockEntity machine) {
        this.machine = machine;
    }

    public int stored() {
        return energy;
    }

    public void setStored(int value) {
        energy = Math.max(0, Math.min(value, getMaxEnergyStored()));
    }

    public void clampToCapacity() {
        int cap = getMaxEnergyStored();
        if (energy > cap) {
            energy = cap;
        }
    }

    public int addInternal(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, Math.max(0, getMaxEnergyStored() - energy));
        energy += accepted;
        return accepted;
    }

    public boolean tryConsume(int amount) {
        if (amount <= 0) {
            return true;
        }
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        return true;
    }

    public boolean hasRoom() {
        return energy < getMaxEnergyStored();
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        if (maxReceive <= 0 || machine.energyModule() != EFEnergyModule.ELECTRIC) {
            return 0;
        }
        int accepted = Math.min(maxReceive, Math.max(0, getMaxEnergyStored() - energy));
        if (!simulate) {
            energy += accepted;
            if (accepted > 0) {
                machine.onEnergyChanged();
            }
        }
        return accepted;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        if (maxExtract <= 0 || machine.energyModule() != EFEnergyModule.GENERATOR) {
            return 0;
        }
        int extracted = Math.min(maxExtract, energy);
        if (!simulate) {
            energy -= extracted;
            if (extracted > 0) {
                machine.onEnergyChanged();
            }
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return machine.energyCapacity();
    }

    @Override
    public boolean canExtract() {
        return machine.energyModule() == EFEnergyModule.GENERATOR && energy > 0;
    }

    @Override
    public boolean canReceive() {
        return machine.energyModule() == EFEnergyModule.ELECTRIC && hasRoom();
    }
}
