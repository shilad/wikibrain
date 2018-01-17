ssh -i ~/Documents/feb2016-keypair.pem ec2-user@ec2-34-192-219-149.compute-1.amazonaws.com


WB_LANG=en

# Basics: git, etc
sudo yum install git screen

# Setup postgres
sudo yum -y install postgresql postgresql-server postgresql-devel postgresql-contrib postgresql-docs
sudo service postgresql initdb
cat >>/var/lib/pgsql9/data/postgresql.conf  << HERE
listen_addresses = '*'
max_connections = 500         # Must be at least 300
shared_buffers = 48GB         # Should be 1/4 of system memory
effective_cache_size = 96GB   # Should be 1/2 of system memory
fsync = off                 
synchronous_commit = off    
checkpoint_segments = 256
checkpoint_completion_target = 0.9
autovacuum = off
HERE

sudo /sbin/chkconfig --levels 235 postgresql on
sudo service postgresql start

createuser -s -r -d -P wikibrain
createdb wikibrain_${WB_LANG}

# Setup java
 wget --no-check-certificate --no-cookies --header "Cookie: oraclelicense=accept-securebackup-cookie" http://download.oracle.com/otn-pub/java/jdk/8u141-b15/336fa29ff2bb4ef291e347e091f7f4a7/jdk-8u141-linux-x64.rpm

# maven: https://gist.github.com/sebsto/19b99f1fa1f32cae5d00
sudo wget http://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo
sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven

# Wikibrain setup
cat >./wmf_${WB_LANG}.conf <<HERE
    baseDir : /home/ec2-user/wikibrain/base_${WB_LANG}
    dao.dataSource.default : psql
    dao.dataSource.psql {
        username : wikibrain
        password : wikibrain
        url : "jdbc:postgresql://localhost/wikibrain_${WB_LANG}"
    }
HERE
mkdir -p /home/ec2-user/wikibrain/base_${WB_LANG}

# Do it!
export JAVA_OPTS="-d64 -Xmx120000M -server"
./wb-java.sh org.wikibrain.Loader -l ${WB_LANG} -c wmf_${WB_LANG}.conf
