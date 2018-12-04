#!/bin/sh

/usr/bin/systemctl stop trellis-aws-rds
/usr/bin/systemctl disable trellis-aws-rds
/usr/bin/systemctl daemon-reload
/usr/bin/systemctl reset-failed
