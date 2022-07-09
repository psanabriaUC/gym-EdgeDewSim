#!/bin/bash

function convertRawToSimData {

	java -cp rawData2SimData.jar cl.puc.ing.edgedewsim.Main $1/raw_samples/CpuBatProfile0.txt $1/battery0_withOffset.cnf $1/cpu0_withOffset.cnf
	java -cp rawData2SimData.jar cl.puc.ing.edgedewsim.Main $1/raw_samples/CpuBatProfile30.txt $1/battery30_withOffset.cnf $1/cpu30_withOffset.cnf
	java -cp rawData2SimData.jar cl.puc.ing.edgedewsim.Main $1/raw_samples/CpuBatProfile75.txt $1/battery75_withOffset.cnf $1/cpu75_withOffset.cnf
	java -cp rawData2SimData.jar cl.puc.ing.edgedewsim.Main $1/raw_samples/CpuBatProfile100.txt $1/battery100_withOffset.cnf $1/cpu100_withOffset.cnf
}

function removeOffset {
	java -jar profileOffsetRemover.jar $1/battery0_withOffset.cnf $1/battery0.cnf
	java -jar profileOffsetRemover.jar $1/cpu0_withOffset.cnf $1/cpu0.cnf
	java -jar profileOffsetRemover.jar $1/battery30_withOffset.cnf $1/battery30.cnf
	java -jar profileOffsetRemover.jar $1/cpu30_withOffset.cnf $1/cpu30.cnf
	java -jar profileOffsetRemover.jar $1/battery75_withOffset.cnf $1/battery75.cnf
	java -jar profileOffsetRemover.jar $1/cpu75_withOffset.cnf $1/cpu75.cnf
	java -jar profileOffsetRemover.jar $1/battery100_withOffset.cnf $1/battery100.cnf
	java -jar profileOffsetRemover.jar $1/cpu100_withOffset.cnf $1/cpu100.cnf
	rm $1/battery0_withOffset.cnf
	rm $1/cpu0_withOffset.cnf
	rm $1/battery30_withOffset.cnf
	rm $1/cpu30_withOffset.cnf
	rm $1/battery75_withOffset.cnf
	rm $1/cpu75_withOffset.cnf
	rm $1/battery100_withOffset.cnf
	rm $1/cpu100_withOffset.cnf
}

function interpolateBatterySamples {
	java -cp batteryInterpolator.jar cl.puc.ing.edgedewsim.batteryInterpolator.BatInter $1/battery0.cnf > $1/battery0_temp.cnf
	java -cp batteryInterpolator.jar cl.puc.ing.edgedewsim.batteryInterpolator.BatInter $1/battery30.cnf > $1/battery30_temp.cnf
	java -cp batteryInterpolator.jar cl.puc.ing.edgedewsim.batteryInterpolator.BatInter $1/battery75.cnf > $1/battery75_temp.cnf
	java -cp batteryInterpolator.jar cl.puc.ing.edgedewsim.batteryInterpolator.BatInter $1/battery100.cnf > $1/battery100_temp.cnf

	cat $1/battery0_temp.cnf > $1/battery0.cnf
	rm $1/battery0_temp.cnf
	cat $1/battery30_temp.cnf > $1/battery30.cnf
	rm $1/battery30_temp.cnf
	cat $1/battery75_temp.cnf > $1/battery75.cnf
	rm $1/battery75_temp.cnf
	cat $1/battery100_temp.cnf > $1/battery100.cnf
	rm $1/battery100_temp.cnf
}

device="MotoG1"

convertRawToSimData ../$device
removeOffset ../$device
interpolateBatterySamples ../$device
