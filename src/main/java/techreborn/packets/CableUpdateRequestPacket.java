package techreborn.packets;

import com.elytradev.concrete.network.Message;
import com.elytradev.concrete.network.NetworkContext;
import com.elytradev.concrete.network.annotation.field.MarshalledAs;
import com.elytradev.concrete.network.annotation.type.ReceivedOn;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import techreborn.Core;
import techreborn.tiles.cable.TileCable;

@ReceivedOn(Side.SERVER)
public class CableUpdateRequestPacket extends Message {
	@MarshalledAs("i32")
	private int dimensionId;
	@MarshalledAs("i32")
	private int x;
	@MarshalledAs("i32")
	private int y;
	@MarshalledAs("i32")
	private int z;

	public CableUpdateRequestPacket(final NetworkContext ctx) {
		super(ctx);
	}

	public CableUpdateRequestPacket(final int dimensionID, final int x, final int y, final int z) {
		this(Core.network);

		this.dimensionId = dimensionID;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	@Override
	protected void handle(final EntityPlayer sender) {
		final BlockPos pos = new BlockPos(this.x, this.y, this.z);
		if (sender.getEntityWorld().provider.getDimension() == this.dimensionId
			&& sender.getEntityWorld().getTileEntity(pos) != null)
		{
			TileCable cable = (TileCable) sender.getEntityWorld().getTileEntity(pos);
			new  CableUpdatePacket(cable).sendToAllWatching(cable);
		}
	}
}
