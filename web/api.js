// Thin wrappers around fetch() for the three HTTP endpoints used by
// the front-end. Each function returns a Promise of the parsed JSON.

/** GET /graph -> { nodes, edges } */
async function fetchGraph() {
    const res = await fetch("/graph");
    return res.json();
}

/** POST /upload : sends the config file as a form-encoded body. */
async function sendConfig(config) {
    return fetch("/upload", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "config=" + encodeURIComponent(config)
    });
}

/** GET /publish : returns the latest value of every topic. */
async function sendPublish(topic, message) {
    const res = await fetch(`/publish?topic=${topic}&message=${message}`);
    return res.json();
}

/** POST /reset : returns the (now zeroed) value of every topic. */
async function sendReset() {
    const res = await fetch("/reset", { method: "POST" });
    return res.json();
}
