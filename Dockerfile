#编译系统
FROM gradle:jdk8 as builder

ADD . /build/
WORKDIR /build/
RUN ["/build/gradlew","build"]
#到了这里的时候肯定是线上版本了，所以直接打包

FROM java:8-jre

COPY --from=builder /build/build/libs/yapi-exporter-fat-1.0-SNAPSHOT.jar /deploy/exporter.jar

ENTRYPOINT ["java","-jar","/deploy/exporter.jar"]
