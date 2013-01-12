#!/bin/sh

currentDir=`pwd`
cd ../../

max=20

for ((i=0; i<$max; ++i))
do
    java -cp bin org.grid.server.Main $currentDir/benchmarkOpen.game &
    sleep 5
    java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerOne $currentDir/agent1.conf &
    java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerOne $currentDir/agent2.conf &
    echo $i
    wait
done

for ((i=0; i<$max; ++i))
do
    java -cp bin org.grid.server.Main $currentDir/benchmarkClosed.game &
    sleep 5
    java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerOne $currentDir/agent1.conf &
    java -cp bin org.grid.agent.Agent localhost thesis_agents.ExplorerOne $currentDir/agent2.conf &
    echo $i
    wait
done