#!/bin/bash

provisionkey=$1
timeout=${2:-60}

cd /var/backups/iofog
sudo tar -cvzf config_backup.tar.gz -P /etc/iofog

iofogversion=$(sudo yum list iofog | grep iofog | awk '{print $2}' | sed -n 1p)

sudo touch prev_version_data
sudo printf 'ver: '$iofogversion | sudo tee prev_version_data

sudo iofog deprovision
sudo service iofog stop

sudo yum check-update
sudo yum update iofog -y

starttimestamp=$(date +%s)
sudo service iofog start
sleep 1

while [ "$(sudo iofog status | grep ioFog | awk '{printf $4 }')" != "RUNNING" ]; do
	sleep 1
	currenttimestamp=$(date +%s)
	currentdeltatime=$(( $currenttimestamp - $starttimestamp ))
	if [ $currentdeltatime -gt $timeout ]; then
		break
	fi
done

sudo iofog provision $provisionkey
