#!/bin/sh

/usr/bin/systemctl link /opt/trellis/etc/trellis-aws-rds.service
/usr/bin/systemctl daemon-reload

