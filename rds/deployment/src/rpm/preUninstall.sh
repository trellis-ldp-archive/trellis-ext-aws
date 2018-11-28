#!/bin/sh

/usr/bin/systemctl stop trellis-aws
/usr/bin/systemctl disable trellis-aws
/usr/bin/systemctl daemon-reload
/usr/bin/systemctl reset-failed
