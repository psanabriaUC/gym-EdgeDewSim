#!/bin/bash

jobsCount=5000
nodesCount=100

#100 refers to the population size and the boolean value refers to whether the population is initialized with the best known individual (true) or not (false)
popParams=(
'100 true' 
'100 false'
)

#the boolean value refers to whether the tournament is with reposition
parentSelection=(
"TournamentParentSelector 10 false" 
"TournamentParentSelector 10 true"
)

crossoverOperator=(
"UniformCrossoverOperator 0.6" 
"UniformCrossoverOperator 0.8" 
"HalfUniformCrossoverOperator 0.6" 
"HalfUniformCrossoverOperator 0.8" 
"MPointCrossoverOperator 10 0.6" 
"MPointCrossoverOperator 10 0.8"
)

mutationOperator=(
"IncrementalMutationOperator 0.15" 
"IncrementalMutationOperator 0.30" 
"RandomMutationOperator 0.15" 
"RandomMutationOperator 0.30"
)

for (( popParam=0; popParam < ${#popParams[@]}; popParam++ )) do
   for (( parentSelectionValue=0; parentSelectionValue < ${#parentSelection[@]}; parentSelectionValue++)) do
	for (( crossoverOperation=0; crossoverOperation < ${#crossoverOperator[@]}; crossoverOperation++))do
	    for (( mutationOperation=0; mutationOperation < ${#mutationOperator[@]}; mutationOperation++)) do
		confId=$popParam$parentSelectionValue$crossoverOperation$mutationOperation
		echo ";gene_data $jobsCount $nodesCount" > LSHD_$confId.cnf
		echo ";population_params "${popParams[$popParam]} >> LSHD_$confId.cnf
		echo ";termination_condition IncreasedFitnessTimeCondition 60000 300000" >> LSHD_$confId.cnf
		echo ";parent_selection "${parentSelection[$parentSelectionValue]} >> LSHD_$confId.cnf
		echo ";crossover_operator "${crossoverOperator[$crossoverOperation]} >> LSHD_$confId.cnf
		echo ";mutation_operator "${mutationOperator[$mutationOperation]} >> LSHD_$confId.cnf
		echo ";population_replacement DeterministicCrowding" >> LSHD_$confId.cnf
	    done
	done
    done
done
