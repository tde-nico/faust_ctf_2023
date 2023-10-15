#!/bin/bash

mkdir -p bin

export APP="server"
export CONTEXT="debug"

serverRegistryPort=12345

java -Xmx250M -cp 'bin' de.faust.auction.AuctionServer $serverRegistryPort
