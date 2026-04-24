async function publish() {
    const topic = document.getElementById("topic").value;
    const message = document.getElementById("message").value;

    const data = await sendPublish(topic, message);

    renderTopics(data);
    await loadGraph();
}

async function upload() {
    const config = document.getElementById("config").value;

    await sendConfig(config);
    await loadGraph();
}

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