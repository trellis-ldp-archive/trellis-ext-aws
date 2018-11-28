#!/bin/sh
sleep 10
/opt/trellis/bin/trellis-aws-rds db migrate /opt/trellis/etc/config.yml
/opt/trellis/bin/trellis-aws-rds server /opt/trellis/etc/config.yml
