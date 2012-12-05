package thesis_agents;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Membership;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

//java -cp bin org.grid.agent.Agent localhost thesis_agents.AExplorer
@Membership(team = "aExplorer", passphrase = "1")
public class AExplorer extends Explorer{

	@Override
	ConcurrentLinkedQueue<Direction> getPlan(Position target, boolean includeHQ, boolean includeEnemyAgent) {
		return new ConcurrentLinkedQueue<Direction>(localMap.aStarPlan(target, includeHQ, includeEnemyAgent));
	}

	@Override
	int getMessageRadius() {
		return 3;
	}

	@Override
	boolean isKillingEnabled() {
		return false;
	}

}
