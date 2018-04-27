#!/usr/bin/env bash
#
# Launch remote ec2 execution of WikiBrain corpora.
#

if [ $# -ne "1" ]; then
    echo "usage: $0 lang1,lang2,lang3" >&2
    exit 1
fi


set -e
set -x

wb_langs=$1

INSTANCE_TYPE=m5.2xlarge
SECURITY_GROUP=sg-ca3d6c83
SUBNET=subnet-18171730
INSTALL_URL=https://raw.githubusercontent.com/shilad/wikibrain/develop/scripts/aws_install.sh



# Process each language
for wb_lang in $(echo $wb_langs | tr ',' ' '); do

echo "doing language $wb_lang"

cat << EOF >.custom_bootstrap.sh
set -e
set -x

cd /root
wget $INSTALL_URL


EOF

userdata="$(cat .custom_bootstrap.sh | base64 | tr -d '\n' )"


cat << EOF >.launch_specification.json
{
  "EbsOptimized": true,
  "BlockDeviceMappings": [
    {
      "DeviceName": "/dev/xvda",
      "Ebs": {
        "DeleteOnTermination": true
      }
    }
  ],
  "NetworkInterfaces": [
    {
      "DeviceIndex": 0,
      "AssociatePublicIpAddress": true,
      "DeleteOnTermination": true,
      "Description": "",
      "Groups": [
        "sg-448c8d32"
      ],
      "SubnetId": "subnet-18171730"
    }
  ],
  "ImageId": "ami-1853ac65",
  "InstanceType": "INSTANCE_TYPE",
  "KeyName": "feb2016-keypair",
  "Monitoring": {
    "Enabled": false
  },
  "Placement": {
    "AvailabilityZone": "us-east-1e",
    "GroupName": "",
    "Tenancy": "default"
  },
  "UserData" : "${userdata}"
EOF

#    cp -p ./aws/launch_specification.json ./aws/launch_specification_custom.json
#    sed -i "s/USER_DATA/${userdata}/g" ./aws/launch_specification_custom.json

#    aws ec2 request-spot-instances \
#        --valid-until "2018-05-06T02:52:51.000Z" \
#        --instance-interruption-behavior terminate \
#        --type one-time \
#        --instance-count 1 \
#        --spot-price "5.00" \
#        --launch-specification "file://aws/launch_specification_custom.json"

done


