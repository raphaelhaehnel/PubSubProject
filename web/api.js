async function fetchGraph() {
    const res = await fetch("/graph");
    return res.json();
}

async function sendConfig(config) {
    return fetch("/upload", {
        method: "POST",
        headers: { "Content-Type": "application/x-www-form-urlencoded" },
        body: "config=" + encodeURIComponent(config)
    });
}

async function sendPublish(topic, message) {
    const res = await fetch(`/publish?topic=${topic}&message=${message}`);
    return res.json();
}