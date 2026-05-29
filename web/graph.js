// Draws the graph in the browser using Vis.js.
// We keep one DataSet for nodes and one for edges so refreshing the
// graph is just clear() + add().

let network;
let nodes = new vis.DataSet([]);
let edges = new vis.DataSet([]);

/** Builds the Vis.js network inside the #graph div. Call once at startup. */
function initGraph() {
    const container = document.getElementById("graph");

    network = new vis.Network(container, {
        nodes: nodes,
        edges: edges
    }, {
        nodes: { shape: "dot", size: 16 },
        edges: { arrows: "to" },
        physics: true
    });
}

/** Fetches the current graph from the server and replaces the displayed one. */
async function loadGraph() {
    const data = await fetchGraph();

    nodes.clear();
    edges.clear();

    nodes.add(data.nodes);
    edges.add(data.edges);
}
