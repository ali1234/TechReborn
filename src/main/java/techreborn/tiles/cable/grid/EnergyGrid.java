package techreborn.tiles.cable.grid;

import net.minecraftforge.energy.IEnergyStorage;
import techreborn.blocks.cable.EnumCableType;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EnergyGrid extends CableGrid {
	private final Set<IEnergyCable> connectedCables;
	private final EnumCableType type;

	public EnergyGrid(final int identifier, EnumCableType type) {
		super(identifier);

		this.connectedCables = new HashSet<>();
		this.type = type;
	}

	@Override
	CableGrid copy(final int identifier) {
		return new EnergyGrid(identifier, this.type);
	}

	@Override
	boolean canMerge(final CableGrid grid) {
		if (grid instanceof EnergyGrid && ((EnergyGrid) grid).getCableType() == this.type)
			return super.canMerge(grid);
		return false;
	}

	@Override
	void onMerge(final CableGrid grid) {
		this.getConnectedCables().addAll(((EnergyGrid) grid).getConnectedCables());
	}

	@Override
	void onSplit(final CableGrid grid) {
		this.getConnectedCables().addAll(((EnergyGrid) grid).getConnectedCables().stream()
			.filter(this.getCables()::contains).collect(Collectors.toList()));
	}

	@Override
	void tick() {
		super.tick();

		Set<IEnergyStorage> handlers = this.connectedCables.stream().flatMap(cable ->
			cable.getConnectedHandlers().stream()).collect(Collectors.toSet());

		Set<IEnergyStorage> inputs = handlers.stream().filter(IEnergyStorage::canExtract).collect(Collectors.toSet());
		Set<IEnergyStorage> outputs = handlers.stream().filter(IEnergyStorage::canReceive).collect(Collectors.toSet());

		Long toTransfer = inputs.stream().mapToLong(input -> input.extractEnergy(this.type.transferRate, true)).sum();

		if(toTransfer > 0)
		{
			Map<IEnergyStorage, Integer> transferMap = outputs.stream().collect(Collectors.toMap(Function.identity(),
				storage -> storage.receiveEnergy(Integer.MAX_VALUE, true)));
		}
	}

	public void addConnectedCable(final IEnergyCable cable) {
		this.connectedCables.add(cable);
	}

	public void removeConnectedCable(final IEnergyCable cable) {
		this.connectedCables.remove(cable);
	}

	@Override
	public boolean removeCable(final ITileCable cable) {
		if (super.removeCable(cable)) {
			this.removeConnectedCable((IEnergyCable) cable);
			return true;
		}
		return false;
	}

	public Set<IEnergyCable> getConnectedCables() {
		return this.connectedCables;
	}

	public EnumCableType getCableType() {
		return this.type;
	}
}
