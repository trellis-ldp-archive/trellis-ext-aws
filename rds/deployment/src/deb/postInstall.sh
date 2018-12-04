#!/bin/sh

systemctl link /opt/trellis/etc/trellis-aws-rds.service
systemctl daemon-reload
