# Dockerfile for B-Side Backend Server
FROM eclipse-temurin:21-jre-alpine

# Install curl for healthcheck
RUN apk add --no-cache curl

# Metadata
LABEL maintainer="B-Side Development Team"
LABEL description="B-Side Dating App Backend Server"

# Set working directory
WORKDIR /app

# Copy the JAR file from the build context root
COPY server/build/libs/server-all.jar app.jar

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Environment variables (can be overridden)
ENV POCKETBASE_URL=http://localhost:8090
ENV SERVER_PORT=8080
ENV ENVIRONMENT=production

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
