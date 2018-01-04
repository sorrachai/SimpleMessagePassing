#!/bin/bash
yes | sudo apt-get install emacs24
yes | sudo apt-get install ntp

yes | sudo add-apt-repository ppa:webupd8team/java
yes | sudo apt-get update
yes | sudo apt install oracle-java9-installer

sudo apt install oracle-java9-set-default

sudo sysctl -w net.ipv4.tcp_rmem='4096 87380 8388608'
sudo sysctl -w net.ipv4.tcp_wmem='4096 65536 8388608'
sudo sysctl -w net.ipv4.tcp_mem='8388608 8388608 8388608'
sudo sysctl -w net.ipv4.route.flush=1

sudo wget -N  http://YOURHOST.com/simpleMessagePassing.jar
sudo wget -N  http://YOURHOST.com/simpleMessageJgroupConfig.xml

sudo chmod 777 simpleMessageJgroupConfig.xml

aws_public_ipv4=$(wget -qO- http://instance-data/latest/meta-data/public-ipv4)
aws_private_ipv4=$(wget -qO- http://instance-data/latest/meta-data/local-ipv4)
 
sed -i  "s/AWS_PUBLIC_IP/$aws_public_ipv4/g"  simpleMessageJgroupConfig.xml
sed -i "s/AWS_PRIVATE_IP/$aws_private_ipv4/g"  simpleMessageJgroupConfig.xml
