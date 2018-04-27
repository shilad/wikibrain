#!/usr/bin/env bash
#
# Launch remote ec2 execution of WikiBrain corpora.
#

if [ $# -ne "1" ]; then
    echo "usage: $0 lang" >&2
    exit 1
fi


set -e
set -x

wb_lang=$1

# AWS configuration parameters. These are specific to Shilad's AWS account and should be
AWS_SUBNET=subnet-18171730
AWS_AMI_ID=ami-43a15f3e
AWS_KEYPAIR=feb2016-keypair
AWS_REGION=us-east-1e
AWS_SECURITY_GROUP=sg-448c8d32

# Url where the install script for WikiBrain resides.
INSTALL_URL=https://raw.githubusercontent.com/shilad/wikibrain/develop/scripts/aws_install.sh


echo "doing language $wb_lang"

cat << EOF >.custom_bootstrap.sh
#!/usr/bin/env bash

set -e
set -x

cd /root
wget $INSTALL_URL -O ./bootstrap.sh
bash ./bootstrap.sh
cd ./wikibrain
./scripts/build_corpus.sh ${wb_lang}
/sbin/halt

EOF

userdata="$(cat .custom_bootstrap.sh | base64 | tr -d '\n' )"

# Determine parameters for instance type

case $wb_lang in
    en)
        INSTANCE_TYPE=m5.4xlarge
        STORAGE_GBS=400
        SPOT_MAX=5.00
        ;;
    de|fr|es|it|ja|ru|pl|nl|zh|pt)
        INSTANCE_TYPE=m5.4xlarge
        STORAGE_GBS=150
        SPOT_MAX=1.00
        ;;
    sv|vi|ceb|war|uk|ca|no|fi|cs|hu|ko|fa|id|tr|ar)
        INSTANCE_TYPE=m5.2xlarge
        STORAGE_GBS=50
        SPOT_MAX=1.00
        ;;
    *)
        INSTANCE_TYPE=m5.2xlarge
        STORAGE_GBS=20
        SPOT_MAX=1.00
        ;;
esac


cat << EOF >.launch_specification.json
{
  "EbsOptimized": true,
  "BlockDeviceMappings": [
    {
      "DeviceName": "/dev/xvda",
      "Ebs": {
        "DeleteOnTermination": true,
        "VolumeSize" : ${STORAGE_GBS}
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
        "${AWS_SECURITY_GROUP}"
      ],
      "SubnetId": "${AWS_SUBNET}"
    }
  ],
  "ImageId": "${AWS_AMI_ID}",
  "InstanceType": "${INSTANCE_TYPE}",
  "KeyName": "${AWS_KEYPAIR}",
  "Monitoring": {
    "Enabled": false
  },
  "Placement": {
    "AvailabilityZone": "${AWS_REGION}",
    "GroupName": "",
    "Tenancy": "default"
  },
  "UserData" : "${userdata}"
}
EOF

# Valid for 10 days
valid_until=$(date -v '+10d' '+%Y-%m-%dT00:00:00.000Z')

aws ec2 request-spot-instances \
        --valid-until "${valid_until}" \
        --instance-interruption-behavior terminate \
        --type one-time \
        --instance-count 1 \
        --spot-price "${SPOT_MAX}" \
        --launch-specification "file://.launch_specification.json"
