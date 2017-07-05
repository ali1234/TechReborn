package techreborn.tiles.cable.grid;

import com.google.common.collect.Queues;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.util.Queue;

public class CableTickHandler {
	public static final Queue<ILoadable> loadables = Queues.newArrayDeque();

	@SubscribeEvent
	public void serverTick(final TickEvent.ServerTickEvent e) {
		while (CableTickHandler.loadables.peek() != null)
			CableTickHandler.loadables.poll().load();

		GridManager.getInstance().tickGrids();
	}
}
