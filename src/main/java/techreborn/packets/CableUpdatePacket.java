package techreborn.packets;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import techreborn.Core;
import techreborn.tiles.cable.TileCable;

@ReceivedOn(Side.CLIENT)
public class CableUpdatePacket extends Message
{
	@MarshalledAs("nbt")
	NBTTagCompound sourceTag;

	@MarshalledAs("blockpos")
	BlockPos sourcePos;

	public CableUpdatePacket(final NetworkContext ctx)
	{
		super(ctx);
	}

	public CableUpdatePacket(TileCable source)
	{
		super(Core.network);

		this.sourcePos = source.getPos();
		this.sourceTag = source.writeRenderConnections(new NBTTagCompound());
	}

	@Override
	protected void handle(EntityPlayer sender)
	{
		World w = sender.getEntityWorld();

		TileCable source = (TileCable) w.getTileEntity(sourcePos);

		if (source != null)
			source.readRenderConnections(sourceTag);
	}
}
