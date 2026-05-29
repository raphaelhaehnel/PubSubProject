// Handles the config-upload drop zone and the publish / reset buttons.

let selectedFile = null;

/** Stores the picked file and updates the drop-zone label. */
function setSelectedFile(file) {
    if (!file) return;
    selectedFile = file;
    const nameEl = document.getElementById("file-name");
    const zone = document.getElementById("drop-zone");
    nameEl.textContent = file.name;
    zone.classList.add("has-file");
}

/** Wires the drop zone events (click, dragover, drop, manual upload). */
function initDropZone() {
    const zone = document.getElementById("drop-zone");
    const fileInput = document.getElementById("fileInput");

    zone.addEventListener("click", () => fileInput.click());

    zone.addEventListener("dragover", (e) => {
        e.preventDefault(); // needed so the "drop" event fires
        zone.classList.add("drag-over");
    });

    zone.addEventListener("dragleave", () => zone.classList.remove("drag-over"));

    zone.addEventListener("drop", (e) => {
        e.preventDefault();
        zone.classList.remove("drag-over");
        const file = e.dataTransfer.files[0];
        if (file) setSelectedFile(file);
    });

    fileInput.addEventListener("change", () => {
        if (fileInput.files[0]) setSelectedFile(fileInput.files[0]);
    });
}

/** Sends the selected config file to the server and refreshes the graph. */
async function deploy() {
    if (!selectedFile) {
        alert("Please select a config file first.");
        return;
    }
    const text = await selectedFile.text();
    await sendConfig(text);
    await loadGraph();
}

/** Publishes one message and refreshes both the sidebar and the graph. */
async function publish() {
    const topic = document.getElementById("topic").value;
    const message = document.getElementById("message").value;

    const data = await sendPublish(topic, message);

    renderTopics(data);
    await loadGraph();
}

/** Triggers a server-side reset and refreshes the UI. */
async function resetAll() {
    const data = await sendReset();
    renderTopics(data);
    await loadGraph();
}

/** Re-renders the right-side topic cards from a /publish or /reset response. */
function renderTopics(data) {
    const container = document.getElementById("table");

    if (!data.topics || data.topics.length === 0) {
        container.innerHTML = "<div style='color:#888'>No topics yet</div>";
        return;
    }

    let html = "";
    for (const t of data.topics) {
        html += `
            <div class="topic-card">
                <div class="topic-name">${t.name}</div>
                <div class="topic-value">${t.value}</div>
            </div>
        `;
    }
    container.innerHTML = html;
}
