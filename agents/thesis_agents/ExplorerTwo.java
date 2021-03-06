package thesis_agents;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Membership;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

//java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerTwo
@Membership(team = "ExplorerTwo", passphrase = "1")
public class ExplorerTwo extends Explorer{

	Boolean optimalExplore = null;
	Boolean killing = null;
	Integer radius = null;
	
	
	@Override
	ConcurrentLinkedQueue<Direction> getPlan(Position target, boolean includeHQ, boolean includeEnemyAgent) {
		if(getConfigValueForKey("planAlgorithm").equals("a*"))
		{
			return new ConcurrentLinkedQueue<Direction>(localMap.aStarPlan(target, includeHQ, includeEnemyAgent));
		}
		else
		{
			return new ConcurrentLinkedQueue<Direction>(localMap.dijkstraPlan(target, includeHQ, includeEnemyAgent));
		}
	}

	@Override
	int getMessageRadius() {
		if(radius != null)
		{
			return radius.intValue();
		}
		radius = Integer.parseInt(getConfigValueForKey("messageRadius"));
		return radius.intValue();
	}

	@Override
	boolean isKillingEnabled() {
		if(killing != null)
		{
			return killing.booleanValue();
		}

		if(getConfigValueForKey("killing").equals("YES"))
		{
			killing = true;
			return killing;
		}
		else
		{
			killing = false;
			return killing;
		}
	}

	@Override
	boolean findOptimalPathInExploreMode() {
		if(optimalExplore != null)
		{
			return optimalExplore.booleanValue();
		}
		
		if(getConfigValueForKey("optimalExplore").equals("YES"))
		{
			optimalExplore = true;
			return optimalExplore;
		}
		else
		{
			optimalExplore = false;
			return optimalExplore;
		}
	}

}
