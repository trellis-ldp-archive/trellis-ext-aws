#!/bin/sh

systemctl link /opt/trellis/etc/trellis-aws.service
systemctl daemon-reload
