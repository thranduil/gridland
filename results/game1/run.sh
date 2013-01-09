#!/bin/sh

cd ../../

max=20

for ((i=0; i<=$max; ++i))
do
    java -cp bin org.grid.server.Main results/game1/benchmark.game &
    sleep 5
    java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerOne results/game1/agent1.conf &
    java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerOne results/game1/agent2.conf &
    echo $i
    sleep 150
done