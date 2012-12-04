package thesis_agents;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Membership;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

//java -cp bin org.grid.agent.Agent localhost thesis_agents.DExplorer
@Membership(team = "dExplorer", passphrase = "1")
public class DExplorer extends Explorer{

	@Override
	ConcurrentLinkedQueue<Direction> getPlan(Position target, boolean includeHQ) {
		return new ConcurrentLinkedQueue<Direction>(localMap.dijkstraPlan(target, includeHQ));
	}

	@Override
	int getMessageRadius() {
		return 3;
	}

}
