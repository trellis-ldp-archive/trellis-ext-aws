#!/bin/sh

systemctl stop trellis-aws-rds
systemctl disable trellis-aws-rds
systemctl daemon-reload
systemctl reset-failed
