FROM openjdk:8

RUN mkdir -p /src
RUN mkdir -p /lib/bud

WORKDIR /src
# install dependencies
RUN git clone --depth 1 --branch master https://github.com/predatorray/bud.git bud
WORKDIR /src/bud
RUN ./mvnw clean install -Dmaven.test.skip

# build
COPY . /src/bud-web-repl
WORKDIR /src/bud-web-repl
RUN ./mvnw clean package
RUN mv target/bud-web-repl-boot.jar /lib/bud

# clean up
RUN rm -rf /src/bud /src/bud-web-repl
RUN rm -rf /root/.m2

WORKDIR /lib/bud
ENTRYPOINT ["java", "-jar", "/lib/bud/bud-web-repl.jar"]
