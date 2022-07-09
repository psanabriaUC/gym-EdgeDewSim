#!/bin/bash
NAGENTS=24
for (( i=2; i<=$NAGENTS+1; i++ ))
do
   tmux new-window
   tmux send-keys -t :$i ". ../scripts/load_libraries.sh" C-m
   tmux send-keys -t :$i "python example.py" C-m
done
