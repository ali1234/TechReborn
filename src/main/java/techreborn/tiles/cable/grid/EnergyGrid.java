package techreborn.tiles.cable.grid;

import techreborn.blocks.cable.EnumCableType;

import java.util.HashSet;
import java.util.Set;
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

		/*final Set<ISteamHandler> handlers = this.connectedCables.stream()
			.flatMap(pipe -> pipe.getConnectedHandlers().stream()).collect(Collectors.toSet());
		handlers.add(this.tank);

		final double average = handlers.stream().mapToDouble(handler -> handler.getPressure()).average().orElse(0);

		final ISteamHandler[] above = handlers.stream().filter(handler -> handler.getPressure() - average > 0)
			.toArray(ISteamHandler[]::new);
		final ISteamHandler[] below = handlers.stream().filter(handler -> handler.getPressure() - average < 0)
			.toArray(ISteamHandler[]::new);

		final int drained = Stream.of(above).mapToInt(handler ->
		{
			return handler.drainSteam(
				Math.min((int) ((handler.getPressure() - average) * handler.getCapacity()), this.transferCapacity),
				false);
		}).sum();
		int filled = 0;

		for (final ISteamHandler handler : below)
			filled += handler.fillSteam(
				Math.max(drained / below.length, Math.min(
					(int) ((handler.getPressure() - average) * handler.getCapacity()), this.transferCapacity)),
				true);

		for (final ISteamHandler handler : above)
			handler.drainSteam(
				Math.max(filled / above.length, Math.min(
					(int) ((handler.getPressure() - average) * handler.getCapacity()), this.transferCapacity)),
				true);*/
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
