# On OpenJDK 8 Slim Debian Image

FROM openjdk:8-jre-alpine
LABEL maintainer="Aaron Coburn <acoburn@apache.org>"

ADD trellis-aws-rds-*.tar /opt
RUN mv /opt/trellis-aws-rds-* /opt/trellis
COPY command.sh /
COPY config.yml /
RUN chmod +x /command.sh

CMD ["/command.sh"]
