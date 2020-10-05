FROM adoptopenjdk:14-jre-hotspot

MAINTAINER Daniel Lavoie <dlavoie@live.ca>

ADD target/*.jar /opt/pinot/pinot-smoke-tests-server.jar

EXPOSE 8080

CMD ["java", "-jar", "/opt/pinot/pinot-smoke-tests-server.jar"]