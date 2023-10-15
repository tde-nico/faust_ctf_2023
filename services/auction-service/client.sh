#!/bin/bash

mkdir -p bin

export APP="client"

userName="faust"
registryHost="[::1]"
serverRegistryPort=12345

javac -Xlint:unchecked -cp './src' -d bin $(find src/ -name *.java)
java -cp 'bin' de.faust.auction.AuctionClient $userName $registryHost $serverRegistryPort
