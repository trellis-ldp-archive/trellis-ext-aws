#!/bin/sh

/usr/bin/systemctl stop trellis-aws-neptune
/usr/bin/systemctl disable trellis-aws-neptune
/usr/bin/systemctl daemon-reload
/usr/bin/systemctl reset-failed
