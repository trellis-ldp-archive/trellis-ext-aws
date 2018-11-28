#!/bin/sh

systemctl stop trellis-aws
systemctl disable trellis-aws
systemctl daemon-reload
systemctl reset-failed
