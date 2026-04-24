let network;
let nodes = new vis.DataSet([]);
let edges = new vis.DataSet([]);

function initGraph() {
    const container = document.getElementById("graph");

    network = new vis.Network(container, {
        nodes: nodes,
        edges: edges
    }, {
        nodes: {
            shape: "dot",
            size: 16
        },
        edges: {
            arrows: "to"
        },
        physics: true
    });
}

async function loadGraph() {
    const data = await fetchGraph();

    nodes.clear();
    edges.clear();

    nodes.add(data.nodes);
    edges.add(data.edges);
}