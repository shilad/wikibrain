#!/bin/bash

if [ "$EUID" -ne 0 ]
  then echo "Please run as root"
  exit
fi

export DEBIAN_FRONTEND=noninteractive

# Update, etc.
apt-get -yq update
apt-get -yq upgrade
apt-get -yq install unzip zip pigz pbzip2

# Setup postgres
apt-get -yq install postgresql postgresql-contrib

mem_gb=$(free -h | gawk '/Mem:/{print $2}' | rev | cut -c 2- | rev)


cat >> /etc/postgresql/9.5/main/postgresql.conf  << HERE
listen_addresses = '*'
max_connections = 1000                    # Must be at least 300
shared_buffers = $((mem_gb / 4))GB        # Should be 1/4 of system memory
effective_cache_size = $((mem_gb / 2))GB  # Should be 1/2 of system memory
fsync = off                 
synchronous_commit = off    
max_wal_size = 10GB
checkpoint_completion_target = 0.9
autovacuum = off
HERE

systemctl enable postgresql
systemctl start postgresql

su postgres -c "psql -c CREATE\ USER\ wikibrain\ WITH\ SUPERUSER\ PASSWORD\ \'wikibrain\'\ LOGIN;"

# Setup java
add-apt-repository -y ppa:webupd8team/java
apt-get -yq update
echo "oracle-java8-installer shared/accepted-oracle-license-v1-1 select true"  | debconf-set-selections
apt-get -yq install oracle-java8-installer
apt-get -yq install maven

# Wikibrain setup
git clone https://github.com/shilad/wikibrain.git
cd wikibrain/
git checkout develop

# Do this last, because gensim (at least) requires an internet connection
# and it could take a bit of time for it to come up
wget https://bootstrap.pypa.io/get-pip.py
python3 get-pip.py
pip3 install cython awscli