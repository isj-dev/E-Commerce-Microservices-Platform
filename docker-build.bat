@echo off
echo ================================================
echo  Ecommerce MSA - Docker Build Script
echo ================================================
echo.

echo [1/2] Building all Spring Boot JARs...
call gradlew.bat clean bootJar -x test --parallel
if %ERRORLEVEL% neq 0 (
    echo ERROR: Gradle build failed
    exit /b 1
)

echo.
echo [2/2] Building Docker images and starting containers...
docker-compose up --build -d
if %ERRORLEVEL% neq 0 (
    echo ERROR: Docker Compose failed
    exit /b 1
)

echo.
echo ================================================
echo  All services started!
echo ================================================
echo  API Gateway:       http://localhost:9000
echo  Swagger UI:        http://localhost:9000/swagger-ui.html
echo  Eureka Dashboard:  http://localhost:8761
echo  Grafana:           http://localhost:3000  (admin/admin123)
echo  Prometheus:        http://localhost:9090
echo  Zipkin:            http://localhost:9411
echo ================================================
