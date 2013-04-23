#! /bin/bash

# This is the script to be called by init.d.  Rename it at your convenience.
# Put the brootils.sh regular script at the path below.

# This is a funny, nonstandard init.d script which calls
# the real init.d script.  The reason it is done in this
# roundabout way is that restarting brootils could restart
# the ssh tunnel that the admin is logged in through.
# The restart would kill the tunnel, which would kill the
# admin's session, and thus kill any processes he's started
# including brootils.sh.  The result is that, when restarting,
# the script would die/stop executing before it got a chance
# to restart the tunnel.  The nohup fixes this by ensuring 
# brootils.sh will keep running even if our sessions dies
# (hangs up).

nohup /etc/brootils/brootils.sh $1
