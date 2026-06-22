# Stage 1: Build the React Frontend (Node)
FROM node:20-alpine AS ui-build
WORKDIR /build

# Copy ONLY package files first to leverage Docker layer caching
COPY web/package.json web/package-lock.json* ./

RUN npm install

# Copy the rest of the frontend source code and build it
COPY web/ ./
RUN npm run build


# Stage 2: Build the Java Backend (Maven)
FROM maven:3.9-eclipse-temurin-17-alpine AS java-build
WORKDIR /build

# Cache Maven dependencies by going offline before compiling
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy Java source code and compile
COPY src/ ./src/
# Copy-dependencies to gather Jackson and its transitive dependencies
RUN mvn clean compile dependency:copy-dependencies -DincludeScope=runtime


# Final Production Runtime (JRE)
# Use a lightweight JRE (Java Runtime Environment) instead of a full JDK
FROM eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

# Security: Create and use a non-root user
RUN addgroup -S pubsubgroup && adduser -S pubsubuser -G pubsubgroup

# Copy compiled Java classes and downloaded dependencies (.jar files)
COPY --from=java-build /build/target/classes ./classes
COPY --from=java-build /build/target/dependency ./lib

# Copy the compiled React UI into the exact path expected by the Java server
# (Assuming your StaticResourceServlet looks for the "web/dist" folder relative to the root)
COPY --from=ui-build /build/dist ./web/dist

# Grant ownership of the app files to the non-root user
RUN chown -R pubsubuser:pubsubgroup /app

# Switch out of root
USER pubsubuser

# Document the port
EXPOSE 8080

# Start the server (Using Linux path separator ':' to include both classes and libraries)
CMD ["java", "-cp", "classes:lib/*", "Main"]