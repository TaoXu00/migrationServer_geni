#!/bin/bash
sudo docker stop $(sudo ps -a -q)
sudo docker rm $(sudo docker ps -a -q)
sudo rm -r systemfiles/ContainerStop/checkpoint
sudo rm -r systemfiles/ContainerRestore/checkpoint
