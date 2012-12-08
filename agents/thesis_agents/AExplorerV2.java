package thesis_agents;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Membership;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

//java -cp bin org.grid.agent.Agent localhost thesis_agents.AExplorerV2
@Membership(team = "aExplorer2", passphrase = "1")
public class AExplorerV2 extends Explorer{

	@Override
	ConcurrentLinkedQueue<Direction> getPlan(Position target, boolean includeHQ, boolean includeEnemyAgent) {
		return new ConcurrentLinkedQueue<Direction>(localMap.aStarPlan(target, includeHQ, includeEnemyAgent));
	}

	@Override
	int getMessageRadius() {
		return 2;
	}

	@Override
	boolean isKillingEnabled() {
		return false;
	}

}
