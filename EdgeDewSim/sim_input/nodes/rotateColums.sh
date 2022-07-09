OLDIFS=$IFS
IFS='
'
echo "" > invertido

for line in $(cat $1); do
	c1=$(echo $line | cut -d ";" -f1)
	c2=$(echo $line | cut -d ";" -f2)
	echo "$c2 $c1" >> invertido
done

IFS=$OLDIFS