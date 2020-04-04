#!/usr/bin/env python
from fabric import Connection
from fabric import task
from invoke import Exit
from datetime import datetime
import os
import time

projectName = os.getenv("projectName")
server = os.getenv("server")
userName = os.getenv("userName")
eurekaAddress = os.getenv("eurekaAddress")

WORKSPACE = "/home/deploy/jenkins-server/workspace/ifp-poc/"
DEST_PATH = os.getenv("deployPath")
BACKUP_PATH = DEST_PATH + "/backup/"

def deploy(conn):
    try:
        stop(conn)
    except:
        pass
    now = datetime.now()
    date_time = now.strftime("%Y%m%d%H%M%S")
    fileName = projectName + ".jar "
    conn.run("cd " + DEST_PATH + ";if [ -e " + fileName + " ];  then cp " + fileName + BACKUP_PATH + projectName + "_" + date_time + ".jar ; fi")
    conn.put(WORKSPACE + projectName + "/target/" + projectName + ".jar", DEST_PATH)
    start(conn)

def stop(conn):
    print("<<<<<<<<<<<<<< try to stop application " + projectName + " >>>>>>>>>>>>>>")
    conn.run("ps aux | grep " + projectName + " | awk '{print $2}' | xargs kill ")
    r = conn.run("ps aux | grep " + projectName + " | awk '{print $2}' | wc -l ")
    if 3 == int(r.stdout) :
        print("try to sleep 2 seconds and retry to stop application.")
        time.sleep(2)
        stop(conn)
    else:
        print("<<<<<<<<<<<<<< stop application " + projectName + " success >>>>>>>>>>>>>>")

def start(conn):
    print("<<<<<<<<<<<<<< try to start application " + projectName + " >>>>>>>>>>>>>>")
    commandString = "export HOST_NAME=" + server + "; EUREKA_SERVICE_URL=" + eurekaAddress + " nohup java -jar " + projectName + ".jar > logs/" + projectName + ".log &"
    print("execute command:" + commandString)
    with conn.cd(DEST_PATH):
        conn.run("source /etc/profile; " + commandString)

def main():
    conn = Connection(host=server, user=userName, connect_kwargs={ "key_filename": "/home/deploy/.ssh/private.key",},)
    conn.config.run.env = { 'EUREKA_SERVICE_URL':'http://ifp3-dev1:9090', }
    deploy(conn)

if __name__ == "__main__":
    # execute only if run as a script
    main()
