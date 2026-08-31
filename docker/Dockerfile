# temporal-kotlin-sandbox
# Base image for the Temporal Kotlin hands-on lab (Instruqt).
#
# Contains:
#   - Eclipse Temurin JDK 17 (LTS)
#   - Gradle 9.6 (Kotlin plugin resolves the Kotlin compiler as a build dependency)
#   - Temporal CLI (latest)
#   - All six exercise directories baked into /opt/exercises/
#   - Gradle dependency cache pre-warmed (jars, not just metadata)
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

# ─── Gradle 9.6 ───────────────────────────────────────────────────────────────
ARG GRADLE_VERSION=9.6.1
RUN wget -q "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip" \
    -O /tmp/gradle.zip \
    && unzip -q /tmp/gradle.zip -d /opt \
    && ln -s /opt/gradle-${GRADLE_VERSION} /opt/gradle \
    && ln -s /opt/gradle/bin/gradle /usr/local/bin/gradle \
    && rm /tmp/gradle.zip

ENV GRADLE_HOME=/opt/gradle
ENV PATH="${GRADLE_HOME}/bin:${PATH}"

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
#   6_cost_deep_dive/practice/ and 6_cost_deep_dive/solution/
COPY exercises/ /opt/exercises/
RUN chown -R user:user /opt/exercises

# ─── Pre-warm Gradle dependency cache ────────────────────────────────────────
# `gradle dependencies` resolves the dependency GRAPH but never downloads the
# artifact jars, so it does not actually warm anything a compile needs. Running
# a real compileKotlin does: it pulls every jar plus the Kotlin compiler.
#
# Only the solution/ projects are warmed. Every exercise declares identical
# dependencies and Gradle's artifact cache is global, so warming one project
# warms them all — and solutions are reference implementations, so a compile
# failure here is a genuine CI signal rather than an expected practice stub.
USER user
RUN for dir in \
      /opt/exercises/1_converting/solution \
      /opt/exercises/2_child_workflows/solution \
      /opt/exercises/3_parallel_activities/solution \
      /opt/exercises/4_cost_optimization/solution \
      /opt/exercises/5_saga/solution \
      /opt/exercises/6_cost_deep_dive/solution; \
    do \
      echo "Warming Gradle cache for $dir..." && \
      gradle -p "$dir" compileKotlin -q; \
    done \
    && rm -rf /opt/exercises/*/solution/build

# ─── Shell environment ────────────────────────────────────────────────────────
RUN echo 'export TEMPORAL_ADDRESS="${TEMPORAL_ADDRESS:-127.0.0.1:7233}"' >> /home/user/.bashrc \
    && echo 'alias tl="temporal workflow list --address ${TEMPORAL_ADDRESS}"' >> /home/user/.bashrc \
    && echo 'alias gw="gradle"' >> /home/user/.bashrc

USER root

CMD ["/bin/bash"]
