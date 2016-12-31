FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/dv.jar /dv/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/dv/app.jar"]
