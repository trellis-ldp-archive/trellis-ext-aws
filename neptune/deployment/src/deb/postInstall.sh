#!/bin/sh

systemctl link /opt/trellis/etc/trellis-aws-neptune.service
systemctl daemon-reload
