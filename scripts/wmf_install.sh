WB_LANG=en

sudo su - 

# Setup postgres

cat >> /etc/postgresql/9.6/main/tuning.conf  << HERE
max_connections = 1000         # Must be at least 300
shared_buffers = 6GB         # Should be 1/4 of system memory
effective_cache_size = 12GB  # Should be 1/2 of system memory
fsync = off                 
synchronous_commit = off    
max_wal_size = 2GB
checkpoint_completion_target = 0.9
autovacuum = off
HERE
systemctl enable postgresql
systemctl start postgresql

sudo su - postgres
createuser -s -r -d -P wikibrain
createdb wikibrain_${WB_LANG}


# Setup oracle 8 jdk and maven
sudo apt-get install software-properties-common # for apt-add-repository
sudo apt-add-repository "deb http://ppa.launchpad.net/webupd8team/java/ubuntu yakkety main"
sudo apt update
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys C2518248EEA14886
sudo apt-get update
sudo apt-get install maven
sudo apt-get install oracle-java8-installer
sudo apt install oracle-java8-set-default
# Log out and back in to make sure java home is set

# Build wikibrain
git clone https://github.com/shilad/wikibrain.git
cd wikibrain/
git checkout develop
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller

# Run everything
screen # also setup screenlog
export JAVA_OPTS="-d64 -Xmx12000M -server"
cat >./wmf_${WB_LANG}.conf <<HERE
    baseDir : /srv/wikibrain/${WB_LANG}
    dao.dataSource.default : psql
    dao.dataSource.psql {
        username : wikibrain
        password : wikibrain
        url : "jdbc:postgresql://localhost/wikibrain_${WB_LANG}"
    }
HERE
sudo mkdir -p /srv/wikibrain/${WB_LANG}
sudo chown -R shiladsen /srv/wikibrain/${WB_LANG}

# Do it!
./wb-java.sh org.wikibrain.Loader -l ${WB_LANG} -c wmf_${WB_LANG}.conf 
