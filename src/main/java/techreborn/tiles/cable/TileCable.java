/*
 * This file is part of TechReborn, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2017 TechReborn
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package techreborn.tiles.cable;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.FMLCommonHandler;
import techreborn.blocks.cable.BlockCable;
import techreborn.blocks.cable.EnumCableType;
import techreborn.tiles.cable.grid.*;

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;

/**
 * Created by modmuss50 on 19/05/2017.
 */
public class TileCable extends TileEntity implements IEnergyCable, ILoadable {
	private EnumCableType cableType;
	private int grid;

	private EnumMap<EnumFacing, IEnergyCable> adjacentCables;
	private EnumMap<EnumFacing, IEnergyStorage> adjacentHandlers;
	protected final EnumSet<EnumFacing> renderConnections;

	public TileCable() {
		this.grid = -1;
		this.adjacentCables = new EnumMap<>(EnumFacing.class);
		this.adjacentHandlers = new EnumMap<>(EnumFacing.class);

		this.renderConnections = EnumSet.noneOf(EnumFacing.class);
	}

	@Override
	public void readFromNBT(final NBTTagCompound tagCompound) {
		super.readFromNBT(tagCompound);

		final int previousConnections = this.renderConnections.size();

		if (this.isClient()) {
			this.renderConnections.clear();
			for (final EnumFacing facing : EnumFacing.VALUES) {
				if (tagCompound.hasKey("connected" + facing.ordinal()))
					this.renderConnections.add(facing);
			}
			if (this.renderConnections.size() != previousConnections)
				this.updateState();
		}
	}

	@Override
	public NBTTagCompound writeToNBT(final NBTTagCompound tagCompound) {
		super.writeToNBT(tagCompound);

		if (this.isServer()) {
			for (final EnumFacing facing : this.adjacentCables.keySet())
				tagCompound.setBoolean("connected" + facing.ordinal(), true);
			for (final EnumFacing facing : this.adjacentHandlers.keySet())
				tagCompound.setBoolean("connected" + facing.ordinal(), true);
		}
		return tagCompound;
	}

	private EnumCableType getCableType() {
		if (this.cableType == null)
			this.cableType = world.getBlockState(pos).getValue(BlockCable.TYPE);
		return this.cableType;
	}

	@Override
	public BlockPos getBlockPos() {
		return this.getPos();
	}

	@Override
	public EnumFacing[] getConnections() {
		return this.adjacentCables.keySet().stream().toArray(EnumFacing[]::new);
	}

	@Override
	public ITileCable<EnergyGrid> getConnected(EnumFacing facing) {
		return this.adjacentCables.get(facing);
	}

	@Override
	public int getGrid() {
		return this.grid;
	}

	@Override
	public void setGrid(int gridIdentifier) {
		this.grid = gridIdentifier;

		if (this.getGridObject() != null && !this.adjacentHandlers.isEmpty())
			this.getGridObject().addConnectedCable(this);
	}

	@Override
	public boolean canConnect(ITileCable<?> to) {
		return ((TileCable) to).getCableType() == this.getCableType();
	}

	@Override
	public World getBlockWorld() {
		return this.getWorld();
	}

	@Override
	public EnergyGrid createGrid(int nextID) {
		return new EnergyGrid(nextID, this.getCableType());
	}

	public Collection<IEnergyStorage> getConnectedHandlers() {
		return this.adjacentHandlers.values();
	}

	@Override
	public void updateState() {
		if (this.isServer()) {
			//this.sync();
		}
	}

	@Override
	public void connect(final EnumFacing facing, final ITileCable<EnergyGrid> to) {
		this.adjacentCables.put(facing, (IEnergyCable) to);
	}

	@Override
	public void disconnect(final EnumFacing facing) {
		this.adjacentCables.remove(facing);
		this.updateState();
	}

	public void connectHandler(final EnumFacing facing, final IEnergyStorage to, final TileEntity tile) {
		this.adjacentHandlers.put(facing, to);
		this.updateState();

		if (tile != null && tile instanceof IConnectionAware)
			((IConnectionAware) tile).connectTrigger(facing.getOpposite(), this.getGridObject());
	}

	public void disconnectHandler(final EnumFacing facing, final TileEntity tile) {
		this.adjacentHandlers.remove(facing);
		this.updateState();

		if (tile != null && tile instanceof IConnectionAware)
			((IConnectionAware) tile).disconnectTrigger(facing.getOpposite(), this.getGridObject());
	}

	public void disconnectItself() {
		GridManager.getInstance().disconnectCable(this);

		this.adjacentHandlers.keySet().forEach(facing ->
		{
			final TileEntity handler = this.getBlockWorld().getTileEntity(this.getBlockPos().offset(facing));
			if (handler != null && handler instanceof IConnectionAware)
				((IConnectionAware) handler).disconnectTrigger(facing.getOpposite(), this.getGridObject());
		});
	}

	@Override
	public void onChunkUnload() {
		this.disconnectItself();
	}

	@Override
	public void onLoad() {
		super.onLoad();
		if (!this.world.isRemote && this.getGrid() == -1)
			CableTickHandler.loadables.add(this);
		else if (this.isClient()) {
			//this.forceSync();
		}
	}

	@Override
	public void load() {
		GridManager.getInstance().connectCable(this);
		for (final EnumFacing facing : EnumFacing.VALUES)
			this.scanHandlers(this.pos.offset(facing));
	}

	public void scanHandlers(final BlockPos posNeighbor) {
		final TileEntity tile = this.world.getTileEntity(posNeighbor);

		final BlockPos substract = posNeighbor.subtract(this.pos);
		final EnumFacing facing = EnumFacing.getFacingFromVector(substract.getX(), substract.getY(), substract.getZ())
			.getOpposite();

		if (this.adjacentHandlers.containsKey(facing.getOpposite())) {
			if (tile == null || !tile.hasCapability(CapabilityEnergy.ENERGY, facing)) {
				this.disconnectHandler(facing.getOpposite(), tile);
				if (this.adjacentHandlers.isEmpty())
					this.getGridObject().removeConnectedCable(this);
			} else if (tile.hasCapability(CapabilityEnergy.ENERGY, facing) && !tile
				.getCapability(CapabilityEnergy.ENERGY, facing).equals(this.adjacentHandlers.get(facing.getOpposite()))) {
				this.connectHandler(facing.getOpposite(), tile.getCapability(CapabilityEnergy.ENERGY, facing), tile);
				if (this.getGridObject() != null)
					this.getGridObject().addConnectedCable(this);
			}
		} else {
			if (tile != null && tile.hasCapability(CapabilityEnergy.ENERGY, facing) && !(tile instanceof TileCable)) {
				this.connectHandler(facing.getOpposite(), tile.getCapability(CapabilityEnergy.ENERGY, facing), tile);
				if (this.getGridObject() != null)
					this.getGridObject().addConnectedCable(this);
			}
		}
	}

	public boolean isServer() {
		if (this.world != null)
			return !this.world.isRemote;
		return FMLCommonHandler.instance().getEffectiveSide().isServer();
	}

	public boolean isClient() {
		if (this.world != null)
			return this.world.isRemote;
		return FMLCommonHandler.instance().getEffectiveSide().isClient();
	}

	public boolean isConnected(final EnumFacing facing)
	{
		return this.renderConnections.contains(facing);
	}
}
