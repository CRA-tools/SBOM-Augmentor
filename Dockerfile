FROM sbtscala/scala-sbt:eclipse-temurin-17.0.15_6_1.12.11_3.8.3
LABEL authors="mvdcamme"

RUN apt update
RUN apt install -y python3 python3-pip python3-venv python3-full

WORKDIR /usr/app
RUN python3 -m pip install --break-system-packages cyclonedx-bom

COPY . .
RUN sbt clean compile assembly

ENTRYPOINT ["./augmentor_rest_api"]