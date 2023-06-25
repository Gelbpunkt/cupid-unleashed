#!/bin/sh

while [ getprop vendor.post_boot.parsed != "1" ]
do
    sleep 1
done

/system/bin/cupid-unleashed
