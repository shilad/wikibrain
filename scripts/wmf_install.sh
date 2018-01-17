WB_LANG=en

sudo puppet agent -tv

# Setup postgres
sudo su - postgres
createuser -s -r -d -P wikibrain
createdb wikibrain_${WB_LANG}
cat >./wmf_${WB_LANG}.conf <<HERE
    baseDir : /srv/wikibrain/${WB_LANG}
    dao.dataSource.default : psql
    dao.dataSource.psql {
        username : wikibrain
        password : wikibrain
        url : "jdbc:postgresql://localhost/wikibrain_${WB_LANG}"
    }
HERE



# Setup oracle 8 jdk and maven
sudo add-apt-repository "deb http://ppa.launchpad.net/webupd8team/java/ubuntu yakkety main"
sudo apt update
apt-key adv --keyserver keyserver.ubuntu.com --recv-keys C2518248EEA14886
sudo apt-get update
sudo apt-get install maven
sudo apt-get install oracle-java8-installer
sudo apt install oracle-java8-set-default

# Prepare Wikibrain
git clone https://github.com/shilad/wikibrain.git
cd wikibrain/
git checkout develop
mvn -f wikibrain-utils/pom.xml clean compile exec:java -Dexec.mainClass=org.wikibrain.utils.ResourceInstaller
export JAVA_OPTS="-d64 -Xmx12000M -server"

sudo mkdir -p /srv/wikibrain/${WB_LANG}
sudo chown -R shiladsen /srv/wikibrain/${WB_LANG}

./wb-java.sh org.wikibrain.Loader -l ${WB_LANG} -c wmf_${WB_LANG}.conf 
