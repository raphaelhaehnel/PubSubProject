# Pub-Sub Project

This project was done during the course **"Advanced Programming"** with Prof. Eliahu Khalastchi, Bar-Ilan University, 2026, second semester.

It implements a full server with a backend implementing a computational graph, an HTTP API, and a web frontend.

The backend implements a **publisher-subscriber** mechanism: publishers can publish messages to specific topics, and subscribers can subscribe to these topics to automatically get new published messages. The agents that perform the computations form a directed graph that the user can visualize and interact with in the browser.

<img width="1897" height="925" alt="app-screenshot" src="src/main/resources/app-screenshot.png" />

---

## Features

- A minimal **HTTP server** written from scratch using `java.net.Socket`, with a small servlet API and a fixed-size worker thread pool.
- A **publisher-subscriber** core: topics, messages, agents, and a thread-safe `TopicManager` singleton.
- **Computational agents** that subscribe to topics, perform a computation, and publish the result. The following implementations are provided:
    - `PlusAgent` — sum of N input topics (`subs[0] + subs[1] + ... + subs[N-1]`)
    - `MulAgent` — product of N input topics (`subs[0] * subs[1] * ... * subs[N-1]`)
    - `AvgAgent` — average of N input topics (`(subs[0] + ... + subs[N-1]) / N`)
    - `DivAgent` — divides two numbers (`subs[0] / subs[1]`, with division-by-zero protection)
    - `IncAgent` — increments a single number by 1
    - `BinOpAgent` — generic binary operation, programmatic use only

  `PlusAgent`, `MulAgent` and `AvgAgent` accept any number of inputs. They wait until they have received at least one value on every input before publishing for the first time.
- A **generic configuration loader** that builds the agent graph from a JSON file, using Java reflection.
- **Robust Error Handling & Rollbacks:** The server safely catches bad configurations. If a syntactically valid JSON introduces a cyclic dependency, the server rejects it and triggers an automatic rollback (clearing the corrupted state from the `TopicManager`) to prevent memory leaks and deadlocks.
- A **graph view**: every topic / agent is shown as a node, with the latest message displayed under the node name.

---

## Requirements

- Java 17 or higher
- Maven (for dependency management; only `jackson-databind` is used)
- Node.js and npm (to build the React frontend)
- A web browser (Chrome recommended)
- An internet connection (the Vis.js library used by the frontend is loaded from an external server)

---

## How to Run

### 1. Build the Web UI

Before starting the Java server, you must compile the React frontend:

1. Open a terminal and navigate to the `web` directory.
2. Run `npm install` to install the frontend dependencies.
3. Run `npm run build` to compile the app. This generates the `dist` folder inside `web` that the Java server uses.

### 2. Run the Java Server

#### IntelliJ IDEA
> 1. Open the project in IntelliJ.
> 2. Let Maven download the dependencies (`jackson-databind`).
> 3. Run `Main.java`.
> 4. Open a browser at <http://localhost:8080/app>.
> 5. Press **Enter** in the terminal where the server is running to stop it.

#### Visual Studio Code
> 1. Open the project folder in VS Code (ensure you have the **Extension Pack for Java** installed).
> 2. Let the Java extension sync the project and download Maven dependencies (`jackson-databind`).
> 3. Open `src/main/java/Main.java` and click the **Run** button (code lens) above the `main` method, or press `F5`.
> 4. Open a browser at <http://localhost:8080/app>.
> 5. Press **Enter** in the terminal where the server is running to stop it.

> [!NOTE]
> This app can be run using Docker containers. For more details, see the [Containerization](#containerization) section.

---

## Usage

The main page is split into three columns:
- **Left** — Upload a JSON configuration and publish messages to topics.
- **Center** — Live view of the computational graph.
- **Right** — List of every topic with its latest value.

### 1. Deploy a configuration
You can deploy a graph configuration using two different modes:
- **File Mode:** Drop a `.json` file into the **"Drop your `.json` config here"** zone (or click to select one).
- **Snippet Mode:** Switch to the **Snippet** tab and paste or type your raw JSON configuration directly into the text area.

Once a file is selected or a snippet is entered, press the green **Deploy Graph** button to update the graph. The button will be disabled if the current configuration is already deployed.
You can also use the yellow **Clear Graph** button to completely remove all nodes and start with a blank canvas.

### 2. Publish a message
Fill in the **Topic** and **Message** inputs and press **Send**. The new value will appear in the topics sidebar, and the graph view will refresh.

### 3. Reset everything
Press the red **"Reset all topics to 0"** button. The server will:
- call `reset()` on every agent (clearing their internal state), and
- publish the message `"0"` on every topic.

After the reset, every topic shows `0.0` and the graph view is refreshed.

### 4. Clear graph
Press the yellow **"Clear Graph"** button. The server will:
- call `clear()` on `TopicManagerSingleton` to clear all topics.
- return `{"nodes": [], "edges": []}` to the UI to clear the graph view.

---

## Configuration JSON Format

A configuration describes a list of agents to instantiate. Each agent has:
- `type` — name of the agent class (must be a class in the `graph` package that has a constructor `(String[] subs, String[] pubs)`).
- `subs` — list of topic names this agent **subscribes to**.
- `pubs` — list of topic names this agent **publishes to**.

### Example

```json
{
  "agents": [
    { "type": "PlusAgent", "subs": ["A", "B"], "pubs": ["C"] },
    { "type": "IncAgent",  "subs": ["C"],      "pubs": ["D"] }
  ]
}
```

### Rules
- Topic names can be any string. They are created automatically the first time they are referenced.
- An agent only computes / publishes once it has received at least one value on each of its input topics.
- The graph must **not contain a cycle** — the server rejects cyclic configurations.
- For `PlusAgent` / `MulAgent` / `AvgAgent`, if the same topic name appears more than once in `subs`, it counts multiple times.

---

## Architectural Notes (Deviations from Base Spec)

We did made a few modifications and improvements from the base requirements:

- **Modern Web UI (React vs. iFrames):** Instead of using 3 static `iframe` tags, the frontend is built as a unified React application (`web` directory). It provides a seamless, dynamic user experience without full-page reloads, utilizing `vis-network`'s advanced repulsion physics for clean graph rendering.
- **RESTful JSON API vs. HTML Servlets:** To ensure strict Client-Server decoupling, the `TopicDisplayer` and `ConfLoader` servlets return `application/json` payloads rather than rendering raw HTML strings.
- **Graceful Error UI:** Instead of crashing or showing raw server logs, the frontend parses custom HTTP error responses (400, 404, etc.) and displays them using a sleek, disposable Toast notification with an animated CSS progress bar.
- **Static Resource Serving:** The server includes a dedicated `StaticResourceServlet` mapped to `/app/` that securely serves the compiled React assets (JS, CSS, HTML) while strictly preventing directory traversal attacks.

---

## Tests

We have implemented a comprehensive test suite using **JUnit 5/Mockito** for the backend and **Vitest** for the frontend to ensure robust code quality across the entire stack.

### Backend Testing
Our backend strategy rigorously validates the core logic, thread safety, and end-to-end functionality:

- **Unit Tests:** Individual logic verification for all agents (including edge cases like division-by-zero and NaN handling), message parsing, and configuration loading.
- **Integration Tests:** Verifying multi-agent graph flows, cycle detection algorithms, and end-to-end HTTP request routing.
- **Stress & Concurrency Testing:** Simulating high-load multi-threaded environments to ensure the `TopicManager` and `ParallelAgent` executions are completely thread-safe and free of race conditions.
- **Edge-Case API Testing:** Ensuring the server safely catches bad JSON payloads, handles missing nodes, and correctly translates them into proper HTTP response codes (400, 404, 500).

**How to run backend tests:**
From the project root directory, run:
```bash
mvn clean test
```

Or, if using an IDE like **IntelliJ IDEA** or **VS Code**, you can navigate to the `src/test/java` directory and run the entire test suite directly through the IDE's built-in Test Runner.

### Frontend Testing
To ensure the stability of our "Fail-Fast" architecture, we use **Vitest** to unit test the client-side DTOs. This ensures that configuration validation logic is correct before any network request is ever made.

- **DTO Validation Tests:** We specifically target `ConfigDTO` and `AgentDTO` to verify they correctly identify invalid JSON syntax, missing mandatory fields (like `agents`, `subs`, `pubs`), and incorrect data types.

**How to run frontend tests:**
1. Navigate to the `web` directory:
   ```bash
   cd web
   ```
2. Install the test dependencies (if not already installed):
   ```bash
   npm install -D vitest
   ```
3. Run the tests:
   ```bash
   npm run test
   ```
*Note: Vitest runs in "watch mode" by default. Use `npx vitest run` if you prefer a single-execution run (e.g., for CI/CD pipelines).*

---

## HTTP API

The HTTP server exposes a small REST-like API.

### `GET /graph`
Returns the current graph as JSON.

```bash
curl http://localhost:8080/graph
```

### `POST /upload`
Uploads a new JSON configuration and replaces the current one. The body must be `application/x-www-form-urlencoded` with a single `config` field.

```bash
curl -X POST http://localhost:8080/upload --data-urlencode "config@config.json"
```

### `GET /publish`
Publishes a message on a topic.

```bash
curl "http://localhost:8080/publish?topic=A&message=3"
```

### `POST /reset`
Resets every agent's internal state and publishes `"0"` on every topic.

```bash
curl -X POST http://localhost:8080/reset
```

### `POST /clear`
Deletes the graph and every topic.

```bash
curl -X POST http://localhost:8080/clear
```

### `GET /app/`
Serves the static React front-end and its compiled assets (HTML / CSS / JS).

---

## Server-Side Error Handling & HTTP Entities

To support robust error handling and ensure predictable API outputs, the web server uses Data Transfer Objects (DTOs) for its data lifecycle:

1. **`server.dtos.HTTPRequest`**: The `RequestParser` parses the raw `Socket` InputStream into this strongly-typed object.
2. **`server.dtos.HTTPResponse`**: Servlets do not interact with `OutputStream`s directly. Instead, they process an `HTTPRequest` and return an `HTTPResponse` DTO containing the status code, headers, and body.
3. **`server.exceptions.HTTPException`**: Servlets throw this exception to gracefully bubble up status codes (like `404 Not Found` or `400 Bad Request`). The core `MyHTTPServer` intercepts these exceptions and guarantees a properly formatted `application/json` error response is safely written back to the client.

---

## Project Layout

```text
src/
├── main/java/
│   ├── Main.java                  # Entry point of the application
│   ├── graph/                     # Pub-sub core: topics, agents, messages
│   ├── server/                    # The HTTP server & request parser
│   ├── servlets/                  # One servlet per endpoint
│   └── view/                      # Graph-to-JSON visualizers
│
└── test/java/                     # Automated Test Suite
    ├── graph/                     # Agent logic, Math tests, Stress tests
    ├── servlet/                   # ConfigLoader tests
    └── server/                    # Server routing and HTTP parser tests

web/                               # Modern React frontend
├── src/
│   └── dtos/                      # Client-side validation schemas (ConfigDTO, AgentDTO)
├── App.jsx                        # Main React component & UI logic
├── main.jsx                       # React entry point
├── styles.css                     # External styles and CSS animations
├── index.html                     # Vite HTML template
├── package.json                   # Node dependencies and build scripts
├── vite.config.js                 # Vite bundler configuration
└── dist/                          # Compiled frontend (generated after build)
```

---

## Containerization

The application is fully containerized as a single monolithic image using a three-stage Docker build — the `npm` stage compiles the React frontend, the `Maven` stage compiles the Java backend, and a lightweight `JRE-only` runtime stage assembles the final image.

### Build and Run Locally with Docker
Build the image from source and run it directly:
```bash
# Build the image
docker build -t pubsub-app .

# Run the container
docker run -it --rm -p 8080:8080 pubsub-app
```

Then open your browser at http://localhost:8080/app.
Press Ctrl+C in the terminal to stop the container.

Alternately, you can load our pre-built image and simply run:
```bash
docker run -it --rm -p 8080:8080 saridga/biu-pubsub-app:latest
``` 

### Run with Docker Compose (Pre-built Image)
To pull the published image from Docker Hub and run it without building locally:
```bash
docker compose up
```
This pulls `saridga/biu-pubsub-app:latest` automatically if it isn't already cached on your machine. To stop and remove the container:
```bash
docker compose down
```

###  Build the Image Locally with Docker Compose
To force Docker Compose to build the image from the local `Dockerfile` instead of pulling it:
```bash
docker compose up --build
```
This rebuilds the image from scratch every time the flag is passed, which is useful after making source code changes. To run in detached (background) mode:
```bash
docker compose up --build -d
```
To stop:
```bash
docker compose down
```

> [!NOTE]
> All three options serve the app on port `8080`. Make sure no other process is occupying that port before starting the container. If you need a different port, change the left-hand side of the `ports` mapping in `docker-compose.yaml` (e.g., `"9090:8080"`).

---

## Writing Your Own Agent

To add a new agent:

1. Create a new class in the `graph` package that implements `Agent`.
2. Provide a public constructor with the exact signature `public MyAgent(String[] subs, String[] pubs)`.
3. In the constructor, subscribe to the topics you need and register as publisher of the output topic(s).
4. Implement `callback(String topic, Message msg)` with your logic.
5. Reference your new class from the JSON config with `"type": "MyAgent"` — `GenericConfig` will pick it up automatically.