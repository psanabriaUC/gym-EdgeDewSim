load "defaults.p"
set encoding iso_8859_1
set terminal postscript eps monochrome noenhanced defaultplex "Arial" 12
set out "out.eps"


set grid noxtics ytics
#show size
set key top right horizontal
set key outside 
set xtics rotate by -90 border in scale 1,0.5 nomirror font "Arial,9"

set ylabel "CPU Usage"
set xlabel "time in seconds"
set datafile separator ";"
plot "fileIn" using ($2/1000):4 with steps ls 2 title "cpu usage"

