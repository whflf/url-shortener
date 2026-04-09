# Distributed URL Shortener & Analytics Ecosystem

### 🚀 Project Overview
A microservice-based URL shortening system designed for scalability and low latency.

### 🏗 System Architecture
The system is decomposed into three independent services to ensure high availability and isolation:

1.  **Shortener Service (Java 21, Spring Boot 4.0.3)**
    *   Handles URL shortening and redirection logic.
    *   Implemented **Redis Caching** to achieve sub-10ms redirection latency.
    *   Acts as a **Kafka Producer**, emitting click events for downstream processing.
2.  **Analytics Service (Java 21, Spring Boot 4.0.3)**
    *   **Kafka Consumer**: Processes stream data (User-Agent, Referer, Device Type) asynchronously.
    *   Stores detailed click analytics in a dedicated PostgreSQL database.
    *   Exposes custom business metrics for Prometheus.
3.  **Safety Service (Python 3.12)**
    *   **gRPC Server**: Performs heuristic+ML analysis to detect phishing and malicious links.

### 🛠 Tech Stack
*   **Languages:** Java 21 (LTS), Python 3.12
*   **Frameworks:** Spring Boot 4.0.3, Spring Data JPA, gRPC (Protobuf)
*   **Infrastructure:** PostgreSQL, Redis, Apache Kafka
*   **Observability:** Prometheus, Grafana
*   **DevOps:** Docker, Docker Compose

### 📈 Observability (Monitoring)
The project features a full-stack monitoring solution:
*   **Infrastructure Metrics:** JVM heap usage, CPU load, HikariCP connection pool status.
*   **Business Insights:** Real-time counters for clicks categorized by browser, OS, and device type.
*   **Dashboards:** Automated data scraping via Prometheus with visualization in Grafana.

![Link clicks metrics visualization example (Grafana)](images/grafana_example.png)

### 🏁 Quick Start
The entire system is containerized. You only need Docker to run the project:

```bash
# Clone the repository
git clone https://github.com/whflf/url-shortener.git

# Spin up all services and infrastructure
docker-compose up --build
```

**Access Points:**
*   Shortener API: `http://localhost:8080`
*   Analytics API: `http://localhost:8082`
*   Prometheus: `http://localhost:9090`
*   Grafana: `http://localhost:3000` (Credentials: `admin` / `admin`)
