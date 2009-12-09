#!/bin/sh

mkdir $1-save
cp $1/*.wav $1-save
for filename in $1/*.wav
do
   echo trimming $filename
   sox $filename temp1.wav reverse
   sox temp1.wav temp2.wav trim 00:00:00.1
   sox temp2.wav temp3.wav reverse
   rm -f $filename
   ./silencecutter.sh -s 160 -c 8 -t 40 -r 0 temp3.wav $filename
   rm -f temp1.wav, temp2.wav, temp3.wav
done;
