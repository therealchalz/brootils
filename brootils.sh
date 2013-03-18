#! /bin/sh

# Put JSVC in /usr/local/bin
# Put brootils.jar and all of the required libs in /usr/share/java
# Put required config files and stuff in /etc/brootils

PROCESS_NAME=brootils
START_CLASS=ca.brood.brootils.daemon.BrootilsDaemon
HOME_DIR=/etc/brootils
PID_FILE=/var/run/brootils.pid

CPHOME=/usr/share/java
JSVC=/usr/local/bin/jsvc
JVM=/usr/lib/jvm/default-java

CP=$CPHOME/activation.jar:\
$CPHOME/brootils.jar:\
$CPHOME/commons-daemon-1.0.13.jar:\
$CPHOME/jsch-0.1.49.jar:\
$CPHOME/log4j-1.2.16.jar:\
$CPHOME/mail.jar

case "$1" in
        start)
                echo -n "Starting daemon "
				$JSVC -cp $CP -home $JVM -cwd $HOME_DIR -pidfile $PID_FILE -procname $PROCESS_NAME $START_CLASS
                ;;
        stop)
                echo -n "Shutting down daemon "
				$JSVC -stop -cp $CP -home $JVM -cwd $HOME_DIR -pidfile $PID_FILE -procname $PROCESS_NAME $START_CLASS
                ;;
        restart)
                ## Stop the service and regardless of whether it was
                ## running or not, start it again.
                $0 stop
                $0 start
                ;;
        *)
                ## If no parameters are given, print which are avaiable.
                echo "Usage: $0 {start|stop|restart}"
                exit 1
                ;;
esac
