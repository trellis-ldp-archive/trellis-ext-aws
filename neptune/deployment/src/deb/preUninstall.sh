#!/bin/sh

systemctl stop trellis-aws-neptune
systemctl disable trellis-aws-neptune
systemctl daemon-reload
systemctl reset-failed
