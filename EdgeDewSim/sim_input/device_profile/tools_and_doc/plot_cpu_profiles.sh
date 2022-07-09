#!/bin/bash
#@Author: Matias Hirsch
function plotCpuSamples {
    
    for profile in $(ls -AB1 $1/*.cnf | grep "cpu"); do
	node=$(echo $profile | grep "cpu" | cut -d "/" -f4 | cut -d "_" -f1)	
	root=${profile%/*f}
	outFile=$root'\/'$node'Cpu.eps'
	sed -i 's|out.eps|'$outFile'|g' metric.plot
	sed -i 's|fileIn|'$profile'|g' metric.plot	
    	gnuplot metric.plot
    	sleep 3	
	#reset to plot default values
	sed -i 's|'$outFile'|out.eps|g' metric.plot
    	sed -i 's|'$profile'|fileIn|g' metric.plot
    done
	
}

device=(
"A100" 
"ViewPad"
"I550" 
"L9" 
"GalaxySIII" 
"GalaxyTab2" 
)

for (( i=0; i<${#device[@]}; i++)) do
    echo "plotting device ${device[$i]} in:"
    echo "topology 10A10064ViewPad26i5500"
    plotCpuSamples ../${device[$i]}"/10A10064ViewPad26i5500"
    echo "topology 20A10041ViewPad39i5500"
    plotCpuSamples ../${device[$i]}"/20A10041ViewPad39i5500"
    echo "topology 30A10018ViewPad52i5500"
    plotCpuSamples ../${device[$i]}"/30A10018ViewPad52i5500"    
done
