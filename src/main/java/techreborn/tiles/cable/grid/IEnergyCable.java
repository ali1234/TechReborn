package techreborn.tiles.cable.grid;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.Collection;

public interface IEnergyCable extends ITileCable<EnergyGrid> {
	Collection<IEnergyStorage> getConnectedHandlers();
}
