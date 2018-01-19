ssh -i ~/Documents/feb2016-keypair.pem ec2-user@ec2-34-192-219-149.compute-1.amazonaws.com

----- Ubuntu -----

WB_LANG=en

sudo su - 

# Update, etc.
apt-get update
apt-get upgrade
apt-get install unzip zip pigz

# Setup postgres
apt-get install postgresql postgresql-contrib
vim /etc/postgresql/9.5/main/pg_hba.conf # change dd

cat >> /etc/postgresql/9.5/main/postgresql.conf  << HERE
listen_addresses = '*'
max_connections = 1000         # Must be at least 300
shared_buffers = 48GB         # Should be 1/4 of system memory
effective_cache_size = 96GB   # Should be 1/2 of system memory
fsync = off                 
synchronous_commit = off    
max_wal_size = 10GB
checkpoint_completion_target = 0.9
autovacuum = off
HERE

systemctl enable postgresql
systemctl start postgresql

su - postgres
createuser -s -r -d -P wikibrain
createdb wikibrain_${WB_LANG}

exit    # will be root
exit    # will be ec2-user

# Setup java
curl -s "https://get.sdkman.io" | bash
source "/home/ubuntu/.sdkman/bin/sdkman-init.sh"
sdk install java 8u161-oracle
sdk install maven

# Wikibrain setup
git clone https://github.com/shilad/wikibrain.git
cd wikibrain/
git checkout develop
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller
export JAVA_OPTS="-d64 -Xmx120000M -server"
cat >./wmf_${WB_LANG}.conf <<HERE
    baseDir : /home/ec2-user/wikibrain/base_${WB_LANG}
    dao.dataSource.default : psql
    dao.dataSource.psql {
        username : wikibrain
        password : wikibrain
        connectionsPerPartition : 5
        url : "jdbc:postgresql://localhost/wikibrain_${WB_LANG}"
    }
HERE
mkdir -p /home/ec2-user/wikibrain/base_${WB_LANG}

# Do it!
export JAVA_OPTS="-d64 -Xmx120000M -server"
./wb-java.sh org.wikibrain.Loader -l ${WB_LANG} -c wmf_${WB_LANG}.conf

------------------ Amazon Linux AMI --------------


WB_LANG=en

# Basics: git, etc
sudo yum install git screen

# Setup postgres
sudo yum -y install postgresql96 postgresql96-server postgresql96-devel postgresql96-contrib postgresql96-docs
sudo service postgresql96 initdb
cat >>/var/lib/pgsql96/data/postgresql.conf  << HERE
listen_addresses = '*'
max_connections = 5000         # Must be at least 300
shared_buffers = 48GB         # Should be 1/4 of system memory
effective_cache_size = 96GB   # Should be 1/2 of system memory
fsync = off                 
synchronous_commit = off    
max_wal_size = 10GB
checkpoint_completion_target = 0.9
autovacuum = off
HERE

# TODO: in /var/lib/pgsql96/data/pg_hba.conf replace ident with md5

sudo /sbin/chkconfig --levels 235 postgresql on
sudo service postgresql start

sudo su - postgres
createuser -s -r -d -P wikibrain
createdb wikibrain_${WB_LANG}

# Setup java
curl -s "https://get.sdkman.io" | bash

wget --no-cookies --header "Cookie: gpw_e24=xxx; oraclelicense=accept-securebackup-cookie;" "http://download.oracle.com/otn-pub/java/jdk/8u162-b12/0da788060d494f5095bf8624735fa2f1/jdk-8u162-linux-x64.rpm?AuthParam=1516313012_7591aaff3d1e8a18a521e2c260615b84"
sudo rpm -i jdk-8u162-linux-x64.rpm*

# log out and back in of ssh to make sure JAVA_HOME is updated

# maven: https://gist.github.com/sebsto/19b99f1fa1f32cae5d00
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven

# Wikibrain setup
git clone https://github.com/shilad/wikibrain.git
cd wikibrain/
git checkout develop
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller
export WB_LANG=en
cat >./wmf_${WB_LANG}.conf <<HERE
    baseDir : /home/ubuntu/wikibrain/base_${WB_LANG}
    dao.dataSource.default : psql
    dao.dataSource.psql {
        username : wikibrain
        password : wikibrain
        connectionsPerPartition : 5
        url : "jdbc:postgresql://localhost/wikibrain_${WB_LANG}"
    }
HERE
mkdir -p /home/ubuntu/wikibrain/base_${WB_LANG}

# Optional: aws
sudo apt-get install python-pip
pip install awscli --upgrade --user
aws configure

# Do it!
screen # start screen log... CTL-a H
export JAVA_OPTS="-d64 -Xmx64000M -server"
./wb-java.sh org.wikibrain.Loader -l ${WB_LANG} -c wmf_${WB_LANG}.conf
