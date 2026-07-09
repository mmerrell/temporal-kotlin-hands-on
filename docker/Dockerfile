# temporal-java-sandbox
# Base image for the Temporal Java SDK hands-on lab (Instruqt).
#
# Contains:
#   - Eclipse Temurin JDK 17 (LTS)
#   - Maven 3.9
#   - Temporal CLI (latest)
#   - All five exercise directories baked into /opt/exercises/
#   - Maven dependency cache pre-warmed for all five exercises
#
# The Temporal server is started by the Instruqt track setup script using
# `nohup temporal server start-dev`. This image is for the learner's sandbox only.

FROM eclipse-temurin:17-jdk-jammy

# ─── System packages ──────────────────────────────────────────────────────────
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    wget \
    git \
    vim \
    nano \
    less \
    unzip \
    ca-certificates \
    bash-completion \
    && rm -rf /var/lib/apt/lists/*

# ─── Maven 3.9 ────────────────────────────────────────────────────────────────
ARG MAVEN_VERSION=3.9.9
RUN wget -q "https://archive.apache.org/dist/maven/maven-3/${MAVEN_VERSION}/binaries/apache-maven-${MAVEN_VERSION}-bin.tar.gz" \
    -O /tmp/maven.tar.gz \
    && tar -xzf /tmp/maven.tar.gz -C /opt \
    && ln -s /opt/apache-maven-${MAVEN_VERSION} /opt/maven \
    && ln -s /opt/maven/bin/mvn /usr/local/bin/mvn \
    && rm /tmp/maven.tar.gz

ENV MAVEN_HOME=/opt/maven
ENV PATH="${MAVEN_HOME}/bin:${PATH}"

# ─── Temporal CLI ─────────────────────────────────────────────────────────────
ARG TEMPORAL_CLI_VERSION=1.3.0
RUN curl -sSL \
    "https://github.com/temporalio/cli/releases/download/v${TEMPORAL_CLI_VERSION}/temporal_cli_${TEMPORAL_CLI_VERSION}_linux_amd64.tar.gz" \
    | tar -xz -C /usr/local/bin temporal \
    && chmod +x /usr/local/bin/temporal

# ─── Non-root user ────────────────────────────────────────────────────────────
RUN useradd -m -s /bin/bash -u 1001 user
WORKDIR /home/user

# ─── Bake in exercise source code ─────────────────────────────────────────────
# The exercises/ directory at the repo root must contain:
#   1_converting/practice/ and 1_converting/solution/
#   2_child_workflows/practice/ and 2_child_workflows/solution/
#   3_parallel_activities/practice/ and 3_parallel_activities/solution/
#   4_cost_optimization/practice/ and 4_cost_optimization/solution/
#   5_saga/practice/ and 5_saga/solution/
COPY exercises/ /opt/exercises/
RUN chown -R user:user /opt/exercises

# ─── Pre-warm Maven dependency cache ─────────────────────────────────────────
# Resolves all dependencies for all ten pom.xml files (practice + solution)
# so learner containers start without downloading anything.
USER user
RUN for dir in \
      /opt/exercises/1_converting/practice \
      /opt/exercises/1_converting/solution \
      /opt/exercises/2_child_workflows/practice \
      /opt/exercises/2_child_workflows/solution \
      /opt/exercises/3_parallel_activities/practice \
      /opt/exercises/3_parallel_activities/solution \
      /opt/exercises/4_cost_optimization/practice \
      /opt/exercises/4_cost_optimization/solution \
      /opt/exercises/5_saga/practice \
      /opt/exercises/5_saga/solution; \
    do \
      echo "Warming Maven cache for $dir..." && \
      mvn -f "$dir/pom.xml" dependency:resolve -q; \
    done

# ─── Shell environment ────────────────────────────────────────────────────────
RUN echo 'export TEMPORAL_ADDRESS="${TEMPORAL_ADDRESS:-127.0.0.1:7233}"' >> /home/user/.bashrc \
    && echo 'alias tl="temporal workflow list --address ${TEMPORAL_ADDRESS}"' >> /home/user/.bashrc \
    && echo 'alias mvnw="mvn compile exec:java"' >> /home/user/.bashrc

USER root

CMD ["/bin/bash"]
