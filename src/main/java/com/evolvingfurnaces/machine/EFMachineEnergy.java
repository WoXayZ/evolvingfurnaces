package com.evolvingfurnaces.machine;

import net.neoforged.neoforge.transfer.TransferPreconditions;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

public final class EFMachineEnergy implements EnergyHandler {

    private final EvolvingMachineBlockEntity machine;
    private final EnergyJournal journal = new EnergyJournal();
    private int energy;

    public EFMachineEnergy(EvolvingMachineBlockEntity machine) {
        this.machine = machine;
    }

    public int stored() {
        return energy;
    }

    public void setStored(int value) {
        energy = Math.max(0, Math.min(value, getCapacityAsInt()));
    }

    public void clampToCapacity() {
        int cap = getCapacityAsInt();
        if (energy > cap) {
            energy = cap;
        }
    }

    public int addInternal(int amount) {
        if (amount <= 0) {
            return 0;
        }
        int accepted = Math.min(amount, Math.max(0, getCapacityAsInt() - energy));
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
        return energy < getCapacityAsInt();
    }

    @Override
    public long getAmountAsLong() {
        return energy;
    }

    @Override
    public long getCapacityAsLong() {
        return machine.energyCapacity();
    }

    @Override
    public int insert(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount <= 0 || machine.energyModule() != EFEnergyModule.ELECTRIC) {
            return 0;
        }
        int inserted = Math.min(amount, Math.max(0, getCapacityAsInt() - energy));
        if (inserted > 0) {
            journal.updateSnapshots(transaction);
            energy += inserted;
        }
        return inserted;
    }

    @Override
    public int extract(int amount, TransactionContext transaction) {
        TransferPreconditions.checkNonNegative(amount);
        if (amount <= 0 || machine.energyModule() != EFEnergyModule.GENERATOR) {
            return 0;
        }
        int extracted = Math.min(amount, energy);
        if (extracted > 0) {
            journal.updateSnapshots(transaction);
            energy -= extracted;
        }
        return extracted;
    }

    private class EnergyJournal extends SnapshotJournal<Integer> {
        @Override
        protected Integer createSnapshot() {
            return energy;
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            energy = snapshot;
        }

        @Override
        protected void onRootCommit(Integer originalState) {
            if (energy != originalState) {
                machine.onEnergyChanged();
            }
        }
    }
}
